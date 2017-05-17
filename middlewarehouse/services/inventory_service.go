package services

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/async"
	commonErrors "github.com/FoxComm/highlander/middlewarehouse/common/errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/logging"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/jinzhu/gorm"
)

type inventoryService struct {
	stockItemRepo  repositories.IStockItemRepository
	unitRepo       repositories.IStockItemUnitRepository
	summaryService ISummaryService
	txn            *gorm.DB
}

type IInventoryService interface {
	WithTransaction(txn *gorm.DB) IInventoryService
	GetStockItems() ([]*models.StockItem, error)
	GetStockItemById(id uint) (*models.StockItem, error)
	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)
	GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error)
	GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error)

	IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error

	HoldItems(refNum string, skus map[string]int) error
	ReserveItems(refNum string) error
	ReleaseItems(refNum string) error
	ShipItems(refNum string) error
	DeleteItems(refNum string) error
}

type updateItemsStatusFunc func() (int, error)

func NewInventoryService(
	stockItemRepo repositories.IStockItemRepository,
	unitRepo repositories.IStockItemUnitRepository,
	summaryService ISummaryService,
) IInventoryService {

	return &inventoryService{stockItemRepo, unitRepo, summaryService, nil}
}

func (service *inventoryService) WithTransaction(txn *gorm.DB) IInventoryService {
	return &inventoryService{
		stockItemRepo:  service.stockItemRepo,
		unitRepo:       service.unitRepo,
		summaryService: service.summaryService,
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

func (service *inventoryService) HoldItems(refNum string, skus map[string]int) error {
	// map [sku]qty to list of SKUs
	skusList := []string{}
	for code := range skus {
		skusList = append(skusList, code)
	}

	// get stock items associated with SKUs
	items, err := service.getStockItemsBySKUs(skusList)
	if err != nil {
		return err
	}

	// get available units for each stock item
	unitsIds, err := service.getUnitsForOrder(items, skus)
	if err != nil {
		return err
	}

	// updated units with refNum and appropriate status
	count, err := service.getUnitRepo().HoldUnitsInOrder(refNum, unitsIds)
	if err != nil {
		return err
	}

	if count == 0 {
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	// update summary
	stockItemsMap := make(map[uint]int)
	for _, si := range items {
		stockItemsMap[si.ID] = skus[si.SKU]
	}
	statusShift := models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold}

	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
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

func (service *inventoryService) getStockItemsBySKUs(skusList []string) ([]*models.StockItem, error) {
	// get stock items associated with SKUs
	items, err := service.stockItemRepo.GetStockItemsBySKUs(skusList)
	if err != nil {
		return nil, err
	}

	// grab found SKU list from repo
	skusListRepo := []string{}
	for _, item := range items {
		skusListRepo = append(skusListRepo, item.SKU)
	}

	// compare expectations with reality
	aggregateErr := commonErrors.AggregateError{}
	diff := utils.DiffSlices(skusList, skusListRepo)
	if len(diff) > 0 {
		for _, sku := range diff {
			msg := fmt.Sprintf("Entry in table stock_items not found for sku=%s.", sku)
			logging.Log.Warnf(msg)
			aggregateErr.Add(&responses.InvalidSKUItemError{Sku: sku, Debug: msg})
		}

		return nil, &aggregateErr
	}

	return items, nil
}

func (service *inventoryService) getUnitsForOrder(items []*models.StockItem, skus map[string]int) ([]uint, error) {
	aggregateErr := commonErrors.AggregateError{}
	unitsIds := []uint{}
	for _, si := range items {
		ids, err := service.unitRepo.GetStockItemUnitIDs(si.ID, models.StatusOnHand, models.Sellable, skus[si.SKU])
		if err != nil {
			aggregateErr.Add(&responses.InvalidSKUItemError{
				Sku:   si.SKU,
				Debug: fmt.Sprintf("SKU %s is out of stock", si.SKU),
			})
		}

		unitsIds = append(unitsIds, ids...)
	}

	if aggregateErr.Length() > 0 {
		return nil, &aggregateErr
	}

	return unitsIds, nil
}

func (service *inventoryService) checkItemsStatus(refNum string, statusShift models.StatusChange) error {
	units, err := service.unitRepo.GetUnitsInOrder(refNum)
	if err != nil {
		return err
	}

	for _, unit := range units {
		if unit.Status != statusShift.From {
			return fmt.Errorf("Order: %s. Status turn: \"%s\" -> \"%s\". Units in \"%s\" status found.",
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
