package services

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/common/async"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"github.com/jinzhu/gorm"
	"github.com/FoxComm/highlander/middlewarehouse/common/db"
)

type shipmentService struct {
	db                 *gorm.DB
	summaryService     ISummaryService
	activityLogger     IActivityLogger
	updateSummaryAsync bool
}

type IShipmentService interface {
	GetShipmentsByOrder(orderRefNum string) ([]*models.Shipment, exceptions.IException)
	CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException)
	UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException)
	UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, exceptions.IException)
}

func NewShipmentService(db *gorm.DB, summaryService ISummaryService, activityLogger IActivityLogger) IShipmentService {
	return &shipmentService{db, summaryService, activityLogger, true}
}

func (service *shipmentService) GetShipmentsByOrder(referenceNumber string) ([]*models.Shipment, exceptions.IException) {
	repo := repositories.NewShipmentRepository(service.db)
	return repo.GetShipmentsByOrder(referenceNumber)
}

func (service *shipmentService) CreateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	txn := service.db.Begin()

	stockItemCounts := make(map[uint]int)
	unitRepo := repositories.NewStockItemUnitRepository(txn)
	for i, lineItem := range shipment.ShipmentLineItems {
		siu, exception := unitRepo.GetUnitForLineItem(shipment.OrderRefNum, lineItem.SKU)
		if exception != nil {
			txn.Rollback()
			return nil, exception
		}

		if err := txn.Model(siu).Update("status", "reserved").Error; err != nil {
			txn.Rollback()
			return nil, db.NewDatabaseException(err)
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
	result, exception := shipmentRepo.CreateShipment(shipment)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	exception = service.updateSummariesToReserved(stockItemCounts)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	activity, exception := activities.NewShipmentCreated(result, result.CreatedAt)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	if exception := service.activityLogger.Log(activity); exception != nil {
		txn.Rollback()
		return nil, exception
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, db.NewDatabaseException(err)
	}

	return result, nil
}

func (service *shipmentService) UpdateShipment(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {
	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	source, exception := shipmentRepo.GetShipmentByID(shipment.ID)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	shipment.ID = source.ID
	return service.updateShipmentHelper(txn, shipmentRepo, shipment, source)
}

func (service *shipmentService) UpdateShipmentForOrder(shipment *models.Shipment) (*models.Shipment, exceptions.IException) {

	txn := service.db.Begin()

	shipmentRepo := repositories.NewShipmentRepository(txn)
	sources, exception := shipmentRepo.GetShipmentsByOrder(shipment.OrderRefNum)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	if len(sources) != 1 {
		txn.Rollback()
		return nil, exceptions.NewNotImplementedException(errors.New("The order requires exactly one shipment. Multiple shipments is not supported yet."))
	}

	source := sources[0]

	return service.updateShipmentHelper(txn, shipmentRepo, shipment, source)
}

func (service *shipmentService) updateShipmentHelper(txn *gorm.DB, shipmentRepo repositories.IShipmentRepository, shipment *models.Shipment, source *models.Shipment) (*models.Shipment, exceptions.IException) {

	shipment.ID = source.ID

	var exception exceptions.IException
	shipment, exception = shipmentRepo.UpdateShipment(shipment)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	exception = service.handleStatusChange(txn, source, shipment)
	if exception != nil {
		txn.Rollback()
		return nil, exception
	}

	if err := txn.Commit().Error; err != nil {
		txn.Rollback()
		return nil, db.NewDatabaseException(err)
	}

	var activity activities.ISiteActivity
	if source.State != shipment.State && shipment.State == models.ShipmentStateShipped {
		stockItemCounts := make(map[uint]int)
		for _, lineItem := range source.ShipmentLineItems {
			siu := lineItem.StockItemUnit

			// Aggregate which stock items, and how many, have been updated, so that we
			// can update summaries asynchronously at the end.
			if count, ok := stockItemCounts[siu.StockItemID]; ok {
				stockItemCounts[siu.StockItemID] = count + 1
			} else {
				stockItemCounts[siu.StockItemID] = 1
			}
		}

		if exception = service.updateSummariesToShipped(stockItemCounts); exception != nil {
			return nil, exception
		}

		activity, exception = activities.NewShipmentShipped(shipment, shipment.UpdatedAt)
		if exception != nil {
			return nil, exception
		}
	} else {
		activity, exception = activities.NewShipmentUpdated(shipment, shipment.UpdatedAt)
		if exception != nil {
			return nil, exception
		}
	}

	if exception = service.activityLogger.Log(activity); exception != nil {
		return nil, exception
	}

	return shipment, nil

}

func (service *shipmentService) updateSummariesToReserved(stockItemsMap map[uint]int) exceptions.IException {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}
	unitType := models.Sellable

	fn := func() exceptions.IException {
		for id, qty := range stockItemsMap {
			if exception := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); exception != nil {
				return exception
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary creating shipment")
}

func (service *shipmentService) updateSummariesToShipped(stockItemsMap map[uint]int) exceptions.IException {
	statusShift := models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}
	unitType := models.Sellable

	fn := func() exceptions.IException {
		for id, qty := range stockItemsMap {
			if exception := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); exception != nil {
				return exception
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary after shipment")
}

func (service *shipmentService) handleStatusChange(db *gorm.DB, oldShipment, newShipment *models.Shipment) exceptions.IException {
	if oldShipment.State == newShipment.State {
		return nil
	}

	unitRepo := repositories.NewStockItemUnitRepository(db)
	var exception exceptions.IException

	switch newShipment.State {
	case models.ShipmentStateCancelled:
		_, exception = unitRepo.UnsetUnitsInOrder(newShipment.OrderRefNum)

	case models.ShipmentStateShipped:
		// TODO: Bring capture back when we move to the capture consumer
		unitIDs := []uint{}
		for _, lineItem := range newShipment.ShipmentLineItems {
			unitIDs = append(unitIDs, lineItem.StockItemUnitID)
		}
		exception = unitRepo.DeleteUnits(unitIDs)
	}

	return exception
}
