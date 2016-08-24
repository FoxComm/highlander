package services

import (
	"github.com/FoxComm/middlewarehouse/common/async"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
)

type shipmentService struct {
	db                 *gorm.DB
	summaryService     ISummaryService
	updateSummaryAsync bool
}

type IShipmentService interface {
	GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, error)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, error)
}

func NewShipmentService(db *gorm.DB, summaryService ISummaryService) IShipmentService {
	return &shipmentService{db, summaryService, true}
}

func (service *shipmentService) GetShipmentsByReferenceNumber(referenceNumber string) ([]*models.Shipment, error) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByReferenceNumber(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	stockItemCounts := make(map[uint]int)
	unitRepo := repositories.NewStockItemUnitRepository(txn)
	for i, lineItem := range shipment.ShipmentLineItems {
		siu, err := unitRepo.GetUnitForLineItem(shipment.ReferenceNumber, lineItem.SKU)
		if err != nil {
			txn.Rollback()
			return nil, err
		}

		if err := txn.Model(siu).Update("status", "reserved").Error; err != nil {
			txn.Rollback()
			return nil, err
		}

		shipment.ShipmentLineItems[i].StockItemUnitID = siu.ID

		// Aggregate which stock items, and how many, have been updated, so that we
		// can update summaries asynchronously at the end.
		if count, ok := stockItemCounts[siu.StockItemID]; ok {
			stockItemCounts[siu.StockItemID] = count + 1
		} else {
			stockItemCounts[siu.StockItemID] = 1
		}
	}

	shipmentRepo := repositories.NewShipmentRepository(txn)
	result, err := shipmentRepo.CreateShipment(shipment)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	err = service.updateSummariesToReserved(stockItemCounts)
	return result, err
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, error) {
	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	source, err := shipmentRepo.GetShipmentByID(shipment.ID)
	if err != nil {
		txn.Rollback()
		return nil, err
	}

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

	err = txn.Commit().Error
	return shipment, err
}

func (service *shipmentService) updateSummariesToReserved(stockItemsMap map[uint]int) error {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}
	unitType := models.Sellable

	fn := func() error {
		for id, qty := range stockItemsMap {
			if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary creating shipment")
}

func (service *shipmentService) updateSummariesToShipped(stockItemsMap map[uint]int) error {
	statusShift := models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}
	unitType := models.Sellable

	fn := func() error {
		for id, qty := range stockItemsMap {
			if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary after shipment")
}

func (service *shipmentService) handleStatusChange(db *gorm.DB, oldShipment, newShipment *models.Shipment) error {
	if oldShipment.State == newShipment.State {
		return nil
	}

	unitRepo := repositories.NewStockItemUnitRepository(db)
	var err error

	switch newShipment.State {
	case models.ShipmentStateCancelled:
		_, err = unitRepo.UnsetUnitsInOrder(newShipment.ReferenceNumber)
	case models.ShipmentStateShipped:
		unitIDs := []uint{}
		for _, lineItem := range newShipment.ShipmentLineItems {
			unitIDs = append(unitIDs, lineItem.StockItemUnitID)
		}
		err = unitRepo.DeleteUnits(unitIDs)
	}

	return err
}
