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

	IncrementStockItemUnits(id, typeId uint, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id, typeId uint, qty int) error

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
	if _, err := service.stockItemRepo.CreateStockItem(stockItem); err != nil {
		return nil, err
	}

	if err := service.summaryService.CreateStockItemSummary(stockItem.ID); err != nil {
		service.stockItemRepo.DeleteStockItem(stockItem.ID)

		return nil, err
	}

	return stockItem, nil
}

func (service *inventoryService) IncrementStockItemUnits(stockItemId, typeId uint, units []*models.StockItemUnit) error {
	if err := service.unitRepo.CreateUnits(units); err != nil {
		return err
	}

	go service.summaryService.UpdateStockItemSummary(stockItemId, typeId, len(units), models.StatusChange{To: "onHand"})

	return nil
}

func (service *inventoryService) DecrementStockItemUnits(stockItemId, typeId uint, qty int) error {
	unitsIds, err := service.unitRepo.OnHandStockItemUnits(stockItemId, typeId, qty)
	if err != nil {
		return err
	}

	if err := service.unitRepo.DeleteUnits(unitsIds); err != nil {
		return err
	}

	go service.summaryService.UpdateStockItemSummary(stockItemId, typeId, -1*qty, models.StatusChange{To: "onHand"})

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

func (service *inventoryService) updateSummaryOnReserve(items []*models.StockItem, skus map[string]int, typeId uint) error {
	stockItemsMap := map[uint]int{}
	for _, si := range items {
		stockItemsMap[si.ID] = skus[si.SKU]
	}

	statusShift := models.StatusChange{From: "onHand", To: "onHold"}
	for id, qty := range stockItemsMap {
		if err := service.summaryService.UpdateStockItemSummary(id, typeId, qty, statusShift); err != nil {
			return err
		}
	}

	return nil
}

func (service *inventoryService) updateSummaryOnRelease(unitsQty []*models.Release, typeId uint) error {
	for _, item := range unitsQty {
		statusShift := models.StatusChange{From: "onHold", To: "onHand"}
		println(item.StockItemID, typeId, item.Qty, statusShift.From, statusShift.To)

		if err := service.summaryService.UpdateStockItemSummary(item.StockItemID, typeId, item.Qty, statusShift); err != nil {
			return err
		}
	}

	return nil
}
