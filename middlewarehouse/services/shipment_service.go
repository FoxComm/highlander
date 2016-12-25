package services

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db                 *gorm.DB
	inventoryService   IInventoryService
	activityLogger     IActivityLogger
	updateSummaryAsync bool
}

type IShipmentService interface {
	GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(db *gorm.DB, inventoryService IInventoryService, activityLogger IActivityLogger) IShipmentService {
	return &shipmentService{db, inventoryService, activityLogger, true}
}

func (service *shipmentService) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, error) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByOrder(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	unitRepo := repositories.NewStockItemUnitRepository(txn)
	for i, lineItem := range shipment.ShipmentLineItems {
		siu, err := unitRepo.GetUnitForLineItem(shipment.OrderRefNum, lineItem.SKU)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		shipment.ShipmentLineItems[i].StockItemUnitID = siu.ID
	}

	shipmentRepo := repositories.NewShipmentRepository(txn)
	result, err := shipmentRepo.CreateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	activity, err := activities.NewShipmentCreated(result, result.CreatedAt)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := service.inventoryService.WithTransaction(txn).ReserveItems(shipment.OrderRefNum); err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := service.activityLogger.Log(activity); err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	return result, nil
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	source, err := shipmentRepo.GetShipmentByID(shipment.ID)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	shipment.ID = source.ID
	return service.updateShipmentHelper(txn, shipmentRepo, shipment, source)
}

func (service *shipmentService) UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, error) {

	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	sources, err := shipmentRepo.GetShipmentsByOrder(shipment.OrderRefNum)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if len(sources) != 1 {
		txn.Rollback()
		return nil, errors.New("The order requires exactly one shipment. Multiple shipments is not supported yet.")
	}

	source := sources[0]

	return service.updateShipmentHelper(txn, shipmentRepo, shipment, source)
}

func (service *shipmentService) updateShipmentHelper(txn *gorm.DB, shipmentRepo repositories.IShipmentRepository, shipment *models.Shipment, source *models.Shipment) (*models.Shipment, error) {

	shipment.ID = source.ID

	var err error
	shipment, err = shipmentRepo.UpdateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	err = service.handleStatusChange(txn, source, shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err = txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := service.logActivity(source, shipment); err != nil {
		return nil, err
	}

	return shipment, nil

}

func (service *shipmentService) logActivity(original *models.Shipment, updated *models.Shipment) error {
	if !updated.IsUpdated(original) {
		return nil
	}

	var activity activities.ISiteActivity
	var err error

	if original.State != updated.State && updated.State == models.ShipmentStateShipped {
		activity, err = activities.NewShipmentShipped(updated, updated.UpdatedAt)
	} else {
		activity, err = activities.NewShipmentUpdated(updated, updated.UpdatedAt)
	}

	if err != nil {
		return err
	}

	return service.activityLogger.Log(activity)
}

func (service *shipmentService) handleStatusChange(txn *gorm.DB, oldShipment, newShipment *models.Shipment) error {
	if oldShipment.State == newShipment.State {
		return nil
	}

	var err error

	switch newShipment.State {
	case models.ShipmentStateCancelled:
		err = service.inventoryService.WithTransaction(txn).ReleaseItems(newShipment.OrderRefNum)

	case models.ShipmentStateShipped:
		// TODO: Bring capture back when we move to the capture consumer
		err = service.inventoryService.WithTransaction(txn).ShipItems(newShipment.OrderRefNum)
	}

	return err
}
