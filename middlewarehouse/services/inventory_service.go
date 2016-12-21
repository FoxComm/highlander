package services

import (
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/async"
	commonErrors "github.com/FoxComm/highlander/middlewarehouse/common/errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

type inventoryService struct {
	stockItemRepo      repositories.IStockItemRepository
	unitRepo           repositories.IStockItemUnitRepository
	summaryService     ISummaryService
	updateSummaryAsync bool
}

type IInventoryService interface {
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
	DeleteItems(refNum string) error
}

type updateItemsStateFunc func() (int, error)

func NewInventoryService(stockItemRepo repositories.IStockItemRepository, unitRepo repositories.IStockItemUnitRepository,
	summaryService ISummaryService) IInventoryService {

	return &inventoryService{stockItemRepo, unitRepo, summaryService, true}
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
	if err := service.unitRepo.CreateUnits(units); err != nil {
		return err
	}

	return service.updateSummary(map[uint]int{stockItemID: len(units)}, unitType, models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) DecrementStockItemUnits(stockItemID uint, unitType models.UnitType, qty int) error {
	unitsIDs, err := service.unitRepo.GetStockItemUnitIDs(stockItemID, models.StatusOnHand, unitType, qty)
	if err != nil {
		return err
	}

	if err := service.unitRepo.DeleteUnits(unitsIDs); err != nil {
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
	items, err := service.stockItemRepo.GetStockItemsBySKUs(skusList)
	if err != nil {
		return err
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
			msg := fmt.Sprintf("Can't hold items for %s - no stock items found", sku)
			aggregateErr.Add(errors.New(msg))
		}

		return aggregateErr
	}

	// get available units for each stock item
	unitsIds := []uint{}
	for _, si := range items {
		ids, err := service.unitRepo.GetStockItemUnitIDs(si.ID, models.StatusOnHand, models.Sellable, skus[si.SKU])
		if err != nil {
			aggregateErr.Add(err)
		}

		unitsIds = append(unitsIds, ids...)
	}

	if aggregateErr.Length() > 0 {
		return aggregateErr
	}

	// updated units with refNum and appropriate status
	count, err := service.unitRepo.HoldUnitsInOrder(refNum, unitsIds)
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

	return service.updateItemsState(refNum, statusShift, func() (int, error) {
		return service.unitRepo.ReserveUnitsInOrder(refNum)
	})
}

func (service *inventoryService) ReleaseItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusOnHand}

	return service.updateItemsState(refNum, statusShift, func() (int, error) {
		return service.unitRepo.UnsetUnitsInOrder(refNum)
	})
}

func (service *inventoryService) DeleteItems(refNum string) error {
	statusShift := models.StatusChange{From: models.StatusReserved, To: models.StatusShipped}

	return service.updateItemsState(refNum, statusShift, func() (int, error) {
		return service.unitRepo.DeleteUnitsInOrder(refNum)
	})
}

func (service *inventoryService) updateItemsState(refNum string, statusShift models.StatusChange, updateFn updateItemsStateFunc) error {
	// extract stock item ids/qty by refNum
	unitsQty, err := service.unitRepo.GetQtyByRefNum(refNum)
	if err != nil {
		return err
	}

	count, err := updateFn()
	if err != nil {
		return err
	}

	if count == 0 {
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	stockItemsMap := make(map[uint]int)
	for _, item := range unitsQty {
		stockItemsMap[item.StockItemID] = item.Qty
	}
	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
}

func (service *inventoryService) updateSummary(stockItemsMap map[uint]int, unitType models.UnitType, statusShift models.StatusChange) error {
	fn := func() error {
		for id, qty := range stockItemsMap {
			if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
				return err
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, "Error updating stock item summary")
}
