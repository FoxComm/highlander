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

	IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error

	ReserveItems(refNum string, skus map[string]int) error
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
	stockItem, err := service.stockItemRepo.CreateStockItem(stockItem)
	if err != nil {
		return nil, err
	}

	if err := service.summaryService.CreateStockItemSummary(stockItem.ID); err != nil {
		service.stockItemRepo.DeleteStockItem(stockItem.ID)

		return nil, err
	}

	return stockItem, nil
}

func (service *inventoryService) IncrementStockItemUnits(stockItemId uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	if err := service.unitRepo.CreateUnits(units); err != nil {
		return err
	}

	go service.summaryService.UpdateStockItemSummary(stockItemId, unitType, len(units), models.StatusChange{To: models.StatusOnHand})

	return nil
}

func (service *inventoryService) DecrementStockItemUnits(stockItemId uint, unitType models.UnitType, qty int) error {
	unitsIds, err := service.unitRepo.OnHandStockItemUnits(stockItemId, unitType, qty)
	if err != nil {
		return err
	}

	if err := service.unitRepo.DeleteUnits(unitsIds); err != nil {
		return err
	}

	go service.summaryService.UpdateStockItemSummary(stockItemId, unitType, -1*qty, models.StatusChange{To: models.StatusOnHand})

	return nil
}

func (service *inventoryService) ReserveItems(refNum string, skus map[string]int) error {
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
		ids, err := service.unitRepo.OnHandStockItemUnits(si.ID, models.Sellable, skus[si.SKU])
		if err != nil {
			return err
		}

		unitsIds = append(unitsIds, ids...)
	}

	// updated units with refNum and appropriate status
	count, err := service.unitRepo.SetUnitsInOrder(refNum, unitsIds)
	if err != nil {
		return err
	}

	if count == 0 {
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	// updated summary
	go service.updateSummaryOnReserve(items, skus, models.Sellable)

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

	go service.updateSummaryOnRelease(unitsQty, models.Sellable)

	return nil
}

func (service *inventoryService) updateSummaryOnReserve(items []*models.StockItem, skus map[string]int, unitType models.UnitType) error {
	stockItemsMap := map[uint]int{}
	for _, si := range items {
		stockItemsMap[si.ID] = skus[si.SKU]
	}

	statusShift := models.StatusChange{From: models.StatusOnHand, To: models.StatusOnHold}
	for id, qty := range stockItemsMap {
		if err := service.summaryService.UpdateStockItemSummary(id, unitType, qty, statusShift); err != nil {
			return err
		}
	}

	return nil
}

func (service *inventoryService) updateSummaryOnRelease(unitsQty []*models.Release, unitType models.UnitType) error {
	for _, item := range unitsQty {
		statusShift := models.StatusChange{From: models.StatusOnHold, To: models.StatusOnHand}

		if err := service.summaryService.UpdateStockItemSummary(item.StockItemID, unitType, item.Qty, statusShift); err != nil {
			return err
		}
	}

	return nil
}
