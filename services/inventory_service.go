package services

import (
	"errors"
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type inventoryService struct {
	stockItemRepo  repositories.IStockItemRepository
	unitRepo       repositories.IStockItemUnitRepository
	summaryService ISummaryService
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
}

func NewInventoryService(stockItemRepo repositories.IStockItemRepository, unitRepo repositories.IStockItemUnitRepository,
	summaryService ISummaryService) IInventoryService {

	return &inventoryService{stockItemRepo, unitRepo, summaryService}
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

	go service.summaryService.CreateStockItemSummary(stockItem.ID)

	return stockItem, nil
}

func (service *inventoryService) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	return service.stockItemRepo.GetAFSByID(id, unitType)
}

func (service *inventoryService) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	return service.stockItemRepo.GetAFSBySKU(sku, unitType)
}

func (service *inventoryService) IncrementStockItemUnits(stockItemId uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	if err := service.unitRepo.CreateUnits(units); err != nil {
		return err
	}

	return service.summaryService.UpdateStockItemSummary(stockItemId, unitType, len(units), models.StatusChange{To: models.StatusOnHand})
}

func (service *inventoryService) DecrementStockItemUnits(stockItemId uint, unitType models.UnitType, qty int) error {
	unitsIds, err := service.unitRepo.GetStockItemUnitIDs(stockItemId, models.StatusOnHand, unitType, qty)
	if err != nil {
		return err
	}

	if err := service.unitRepo.DeleteUnits(unitsIds); err != nil {
		return err
	}

	go service.summaryService.UpdateStockItemSummary(stockItemId, unitType, -1*qty, models.StatusChange{To: models.StatusOnHand})

	return nil
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

	// not all stock items by SKUs found
	if len(skusList) != len(items) {
		return errors.New("Wrong SKUs list")
	}

	// get available units for each stock item
	unitsIds := []uint{}
	for _, si := range items {
		ids, err := service.unitRepo.GetStockItemUnitIDs(si.ID, models.StatusOnHand, models.Sellable, skus[si.SKU])
		if err != nil {
			return err
		}

		unitsIds = append(unitsIds, ids...)
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
	go service.updateSummary(stockItemsMap, models.Sellable, statusShift)

	return nil
}

func (service *inventoryService) ReserveItems(refNum string) error {
	//get order units
	stockItemUnits, err := service.unitRepo.GetUnitsInOrder(refNum)

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
	count, err := service.unitRepo.ReserveUnitsInOrder(refNum)
	if err != nil {
		return err
	}

	if count == 0 {
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	// updated summary
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusReserved}
	go service.updateSummary(stockItemsMap, models.Sellable, statusShift)

	return nil
}

func (service *inventoryService) ReleaseItems(refNum string) error {
	// extract stock item ids/qty by refNum
	unitsQty, err := service.unitRepo.GetReleaseQtyByRefNum(refNum)
	if err != nil {
		return err
	}

	count, err := service.unitRepo.UnsetUnitsInOrder(refNum)
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
	statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusOnHand}
	go service.updateSummary(stockItemsMap, models.Sellable, statusShift)

	return nil
}

func (service *inventoryService) updateSummary(stockItemsMap map[uint]int, unitType models.UnitType, statusShift models.StatusChange) error {
	for id, qty := range stockItemsMap {
		if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
			return err
		}
	}

	return nil
}
