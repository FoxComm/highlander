package services

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/async"
	commonErrors "github.com/FoxComm/highlander/middlewarehouse/common/errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/logging"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type inventoryService struct {
	stockItemRepo  repositories.IStockItemRepository
	unitRepo       repositories.IStockItemUnitRepository
	summaryService ISummaryService
	db             *gorm.DB
	txn            *gorm.DB
}

// IInventoryService provides an interface for retrieving and manipulating the
// quantities of items in Middlewarehouse.
type IInventoryService interface {
	WithTransaction(txn *gorm.DB) IInventoryService
	GetStockItems() ([]*models.StockItem, error)
	GetStockItemById(id uint) (*models.StockItem, error)
	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)
	GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error)
	GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error)

	IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error

	HoldItems(payload *payloads.Reservation) error
	ReserveItems(refNum string) error
	ReleaseItems(refNum string) error
	ShipItems(refNum string) error
	DeleteItems(refNum string) error
}

type updateItemsStatusFunc func() (int, error)

func NewInventoryService(db *gorm.DB) IInventoryService {
	stockItemRepo := repositories.NewStockItemRepository(db)
	unitRepo := repositories.NewStockItemUnitRepository(db)
	summaryService := NewSummaryService(db)
	return &inventoryService{stockItemRepo, unitRepo, summaryService, db, nil}
}

func (service *inventoryService) WithTransaction(txn *gorm.DB) IInventoryService {
	return &inventoryService{
		stockItemRepo:  service.stockItemRepo,
		unitRepo:       service.unitRepo,
		summaryService: service.summaryService,
		db:             txn,
		txn:            txn,
	}
}

func (service *inventoryService) GetStockItems() ([]*models.StockItem, error) {
	return service.stockItemRepo.GetStockItems()
}

func (service *inventoryService) GetStockItemById(id uint) (*models.StockItem, error) {
	return service.stockItemRepo.GetStockItemById(id)
}

func (service *inventoryService) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	if err := service.stockItemRepo.UpsertStockItem(stockItem); err != nil {
		return nil, err
	}

	err := service.summaryService.CreateStockItemSummary(stockItem.ID)
	if err != nil {
		return nil, err
	}

	return stockItem, nil
}

func (service *inventoryService) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	return service.stockItemRepo.GetAFSByID(id, unitType)
}

func (service *inventoryService) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	return service.stockItemRepo.GetAFSBySKU(sku, unitType)
}

func (service *inventoryService) IncrementStockItemUnits(stockItemID uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	if err := service.getUnitRepo().CreateUnits(units); err != nil {
		return err
	}

	return service.updateSummary(map[uint]int{stockItemID: len(units)}, unitType, models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) DecrementStockItemUnits(stockItemID uint, unitType models.UnitType, qty int) error {
	unitsIDs, err := service.getUnitRepo().GetStockItemUnitIDs(stockItemID, models.StatusOnHand, unitType, qty)
	if err != nil {
		return err
	}

	if err := service.getUnitRepo().DeleteUnits(unitsIDs); err != nil {
		return err
	}

	return service.updateSummary(map[uint]int{stockItemID: -qty}, unitType, models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) HoldItems(payload *payloads.Reservation) error {
	// Iterate through the list of line items and reduce the SKUs into a single
	// map of quantities.
	skuQuantities := map[string]uint{}
	for _, item := range payload.Items {
		_, ok := skuQuantities[item.SKU]
		if ok {
			skuQuantities[item.SKU] += item.Qty
		} else {
			skuQuantities[item.SKU] = item.Qty
		}
	}

	// Iterate through each SKU, and create a hold if the SKU tracks shipping.
	txn := service.db.Begin()
	unitRepo := repositories.NewStockItemUnitRepository(txn)
	summaryService := NewSummaryService(txn)

	aggregateErr := commonErrors.AggregateError{}

	statusShift := models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold}
	for skuCode, qty := range skuQuantities {
		var sku models.SKU
		if err := txn.Where("code = ?", skuCode).First(&sku).Error; err != nil {
			msg := fmt.Sprintf("Entry in table stock_item_units not found for sku=%s.", skuCode)
			logging.Log.Warnf(msg)
			logging.Log.Errorf(err.Error())
			aggregateErr.Add(&responses.InvalidSKUItemError{Sku: skuCode, Afs: 0, Debug: msg})
			continue
		}

		if sku.RequiresInventoryTracking {
			// Hold the units and error is not all can be held.
			units, err := unitRepo.HoldUnits(payload.RefNum, skuCode, qty)
			if err != nil {
				txn.Rollback()
				return err
			}

			if uint(len(units)) != qty {
				defer txn.Rollback()
				debugMessage := fmt.Sprintf(
					"Expected to hold %d units of SKU %s for order %s, but was only able to hold %d",
					qty,
					skuCode,
					payload.RefNum,
					len(units),
				)
				aggregateErr.Add(&responses.InvalidSKUItemError{Sku: skuCode, Afs: len(units), Debug: debugMessage})
				continue
			}

			// Group the stock items and update the summary.
			stockItemQuantities := make(map[uint]int)
			for _, unit := range units {
				_, ok := stockItemQuantities[unit.StockItemID]
				if ok {
					stockItemQuantities[unit.StockItemID]++
				} else {
					stockItemQuantities[unit.StockItemID] = 1
				}
			}

			for stockItemID, stockItemQty := range stockItemQuantities {
				err := summaryService.UpdateStockItemSummary(stockItemID, models.Sellable, stockItemQty, statusShift)
				if err != nil {
					txn.Rollback()
					return err
				}
			}
		}
	}
	if aggregateErr.Length() > 0 {
		txn.Rollback()
		return &aggregateErr
	}
	return txn.Commit().Error
}

func (service *inventoryService) ReserveItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}

	return service.updateItemsStatus(refNum, statusShift, func() (int, error) {
		return service.getUnitRepo().ReserveUnitsInOrder(refNum)
	}, service.txn)
}

func (service *inventoryService) ReleaseItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusOnHand}

	return service.updateItemsStatus(refNum, statusShift, func() (int, error) {
		return service.getUnitRepo().UnsetUnitsInOrder(refNum)
	}, service.txn)
}

func (service *inventoryService) ShipItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}

	return service.updateItemsStatus(refNum, statusShift, func() (int, error) {
		return service.getUnitRepo().ShipUnitsInOrder(refNum)
	}, service.txn)
}

func (service *inventoryService) DeleteItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusOnHand}

	return service.updateItemsStatus(refNum, statusShift, func() (int, error) {
		return service.getUnitRepo().DeleteUnitsInOrder(refNum)
	}, service.txn)
}

func (service *inventoryService) checkItemsStatus(refNum string, statusShift models.StatusChange) error {
	units, err := service.unitRepo.GetUnitsInOrder(refNum)
	if err != nil {
		return err
	}

	for _, unit := range units {
		if unit.Status != statusShift.From {
			return fmt.Errorf("Order: %s. Status turn: \"%s\" -> \"%s\". Units in \"%s\" status found",
				refNum,
				statusShift.From,
				statusShift.To,
				unit.Status,
			)
		}
	}

	return nil
}

func (service *inventoryService) updateItemsStatus(
	refNum string, statusShift models.StatusChange,
	updateFn updateItemsStatusFunc,
	txn *gorm.DB,
) error {
	// extract stock item ids/qty by refNum
	unitsQty, err := service.unitRepo.GetQtyForOrder(refNum)
	if err != nil {
		return err
	}
	if len(unitsQty) == 0 {
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	if err := service.checkItemsStatus(refNum, statusShift); err != nil {
		return err
	}

	if _, err = updateFn(); err != nil {
		return err
	}

	stockItemsMap := make(map[uint]int)
	for _, item := range unitsQty {
		stockItemsMap[item.StockItemID] = item.Qty
	}
	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
}

func (service *inventoryService) updateSummary(stockItemsMap map[uint]int, unitType models.UnitType, statusShift models.StatusChange) error {
	fn := func() error {
		summaryService := service.getSummaryService()

		for id, qty := range stockItemsMap {
			if err := summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, false, "Error updating stock item summary")
}

func (service *inventoryService) getUnitRepo() repositories.IStockItemUnitRepository {
	if service.txn == nil {
		return service.unitRepo
	}

	return service.unitRepo.WithTransaction(service.txn)
}

func (service *inventoryService) getSummaryService() ISummaryService {
	if service.txn == nil {
		return service.summaryService
	}

	return service.summaryService.WithTransaction(service.txn)
}
