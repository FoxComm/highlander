package services

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/async"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
)

const (
	ErrorNoStockItemsForSKU               = "Can't hold items for %s - no stock items found"
	ErrorNoStockItemsAssociatedWithRefNum = "No stock item units associated with %s"
	ErrorUpdatingStockItemSummary         = "Error updating stock item summary"
)

type inventoryService struct {
	stockItemRepo      repositories.IStockItemRepository
	unitRepo           repositories.IStockItemUnitRepository
	summaryService     ISummaryService
	updateSummaryAsync bool
}

type IInventoryService interface {
	GetStockItems() ([]*models.StockItem, exceptions.IException)
	GetStockItemById(id uint) (*models.StockItem, exceptions.IException)
	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, exceptions.IException)
	GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, exceptions.IException)
	GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, exceptions.IException)

	IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) exceptions.IException
	DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) exceptions.IException

	HoldItems(refNum string, skus map[string]int) exceptions.IException
	ReserveItems(refNum string) exceptions.IException
	ReleaseItems(refNum string) exceptions.IException
}

func NewInventoryService(stockItemRepo repositories.IStockItemRepository, unitRepo repositories.IStockItemUnitRepository,
	summaryService ISummaryService) IInventoryService {

	return &inventoryService{stockItemRepo, unitRepo, summaryService, true}
}

func (service *inventoryService) GetStockItems() ([]*models.StockItem, exceptions.IException) {
	return service.stockItemRepo.GetStockItems()
}

func (service *inventoryService) GetStockItemById(id uint) (*models.StockItem, exceptions.IException) {
	return service.stockItemRepo.GetStockItemById(id)
}

func (service *inventoryService) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, exceptions.IException) {
	if exception := service.stockItemRepo.UpsertStockItem(stockItem); exception != nil {
		return nil, exception
	}

	exception := service.summaryService.CreateStockItemSummary(stockItem.ID)
	if exception != nil {
		return nil, exception
	}

	return stockItem, nil
}

func (service *inventoryService) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	return service.stockItemRepo.GetAFSByID(id, unitType)
}

func (service *inventoryService) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	return service.stockItemRepo.GetAFSBySKU(sku, unitType)
}

func (service *inventoryService) IncrementStockItemUnits(stockItemId uint, unitType models.UnitType, units []*models.StockItemUnit) exceptions.IException {
	if exception := service.unitRepo.CreateUnits(units); exception != nil {
		return exception
	}

	return service.updateStockItemSummary(stockItemId, unitType, len(units), models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) DecrementStockItemUnits(stockItemID uint, unitType models.UnitType, qty int) exceptions.IException {
	unitsIDs, exception := service.unitRepo.GetStockItemUnitIDs(stockItemID, models.StatusOnHand, unitType, qty)
	if exception != nil {
		return exception
	}

	if exception := service.unitRepo.DeleteUnits(unitsIDs); exception != nil {
		return exception
	}

	return service.updateStockItemSummary(stockItemID, unitType, -1*qty, models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) HoldItems(refNum string, skus map[string]int) exceptions.IException {
	// map [sku]qty to list of SKUs
	skusList := []string{}
	for code := range skus {
		skusList = append(skusList, code)
	}

	// get stock items associated with SKUs
	items, exception := service.stockItemRepo.GetStockItemsBySKUs(skusList)
	if exception != nil {
		return exception
	}

	// grab found SKU list from repo
	skusListRepo := []string{}
	for _, item := range items {
		skusListRepo = append(skusListRepo, item.SKU)
	}

	// compare expectations with reality
	aggregateException := exceptions.AggregateException{}
	diff := utils.DiffSlices(skusList, skusListRepo)
	if len(diff) > 0 {
		for _, sku := range diff {
			aggregateException.Add(NewNoStockItemsForSKUException(sku, fmt.Errorf(ErrorNoStockItemsForSKU, sku)))
		}

		return aggregateException
	}

	// get available units for each stock item
	unitsIds := []uint{}
	for _, si := range items {
		ids, exception := service.unitRepo.GetStockItemUnitIDs(si.ID, models.StatusOnHand, models.Sellable, skus[si.SKU])
		if exception != nil {
			aggregateException.Add(exception)
		}

		unitsIds = append(unitsIds, ids...)
	}

	if aggregateException.Length() > 0 {
		return aggregateException
	}

	// updated units with refNum and appropriate status
	count, exception := service.unitRepo.HoldUnitsInOrder(refNum, unitsIds)
	if exception != nil {
		return exception
	}

	if count == 0 {
		return NewNoStockItemsAssociatedWithRefNumException(refNum, fmt.Errorf(ErrorNoStockItemsAssociatedWithRefNum, refNum))
	}

	// update summary
	stockItemsMap := make(map[uint]int)
	for _, si := range items {
		stockItemsMap[si.ID] = skus[si.SKU]
	}
	statusShift := models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold}
	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
}

func (service *inventoryService) ReserveItems(refNum string) exceptions.IException {
	//get order units
	stockItemUnits, exception := service.unitRepo.GetUnitsInOrder(refNum)

	// map stockItemUnits to map[stockItem]int
	stockItemsMap := make(map[uint]int)
	for _, stockItemUnit := range stockItemUnits {
		if _, ok := stockItemsMap[stockItemUnit.StockItem.ID]; ok {
			stockItemsMap[stockItemUnit.StockItem.ID]++
		} else {
			stockItemsMap[stockItemUnit.StockItem.ID] = 0
		}
	}

	// updated units with refNum and appropriate status
	count, exception := service.unitRepo.ReserveUnitsInOrder(refNum)
	if exception != nil {
		return exception
	}

	if count == 0 {
		return NewNoStockItemsAssociatedWithRefNumException(refNum, fmt.Errorf(ErrorNoStockItemsAssociatedWithRefNum, refNum))
	}

	// updated summary
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}
	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
}

func (service *inventoryService) ReleaseItems(refNum string) exceptions.IException {
	// extract stock item ids/qty by refNum
	unitsQty, exception := service.unitRepo.GetReleaseQtyByRefNum(refNum)
	if exception != nil {
		return exception
	}

	count, exception := service.unitRepo.UnsetUnitsInOrder(refNum)
	if exception != nil {
		return exception
	}

	if count == 0 {
		return NewNoStockItemsAssociatedWithRefNumException(refNum, fmt.Errorf(ErrorNoStockItemsAssociatedWithRefNum, refNum))
	}

	stockItemsMap := make(map[uint]int)
	for _, item := range unitsQty {
		stockItemsMap[item.StockItemID] = item.Qty
	}
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusOnHand}
	return service.updateSummary(stockItemsMap, models.Sellable, statusShift)
}

func (service *inventoryService) updateStockItemSummary(stockItemID uint, unitType models.UnitType, unitCount int, change models.StatusChange) exceptions.IException {
	fn := func() exceptions.IException {
		return service.summaryService.UpdateStockItemSummary(stockItemID, unitType, unitCount, change)
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, ErrorUpdatingStockItemSummary)
}

func (service *inventoryService) updateSummary(stockItemsMap map[uint]int, unitType models.UnitType, statusShift models.StatusChange) exceptions.IException {
	fn := func() exceptions.IException {
		for id, qty := range stockItemsMap {
			if exception := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); exception != nil {
				return exception
			}
		}

		return nil
	}

	return async.MaybeExecAsync(fn, service.updateSummaryAsync, ErrorUpdatingStockItemSummary)
}

type noStockItemsForSKUException struct {
	Type string `json:"type"`
	SKU  string `json:"sku"`
	exceptions.Exception
}

func (exception noStockItemsForSKUException) ToJSON() interface{} {
	return exception
}

func NewNoStockItemsForSKUException(sku string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return noStockItemsForSKUException{
		Type:      "noStockItemsForSKU",
		SKU:       sku,
		Exception: exceptions.Exception{error.Error()},
	}
}

type noStockItemsAssociatedWithRefNumException struct {
	Type   string `json:"type"`
	RefNum string `json:"refNum"`
	exceptions.Exception
}

func (exception noStockItemsAssociatedWithRefNumException) ToJSON() interface{} {
	return exception
}

func NewNoStockItemsAssociatedWithRefNumException(refNum string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return noStockItemsAssociatedWithRefNumException{
		Type:      "noStockItemsAssociatedWithRefNum",
		RefNum:    refNum,
		Exception: exceptions.Exception{error.Error()},
	}
}
