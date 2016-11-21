package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"

	"log"
	"reflect"
	"strings"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type summaryService struct {
	summaryRepo   repositories.ISummaryRepository
	stockItemRepo repositories.IStockItemRepository
}

type ISummaryService interface {
	CreateStockItemSummary(stockItemId uint) exceptions.IException
	UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) exceptions.IException

	CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) exceptions.IException

	GetSummary() ([]*models.StockItemSummary, exceptions.IException)
	GetSummaryBySKU(sku string) ([]*models.StockItemSummary, exceptions.IException)
}

func NewSummaryService(summaryRepo repositories.ISummaryRepository, stockItemRepo repositories.IStockItemRepository) ISummaryService {
	return &summaryService{summaryRepo, stockItemRepo}
}

func (service *summaryService) GetSummary() ([]*models.StockItemSummary, exceptions.IException) {
	return service.summaryRepo.GetSummary()
}

func (service *summaryService) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, exceptions.IException) {
	return service.summaryRepo.GetSummaryBySKU(sku)
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint) exceptions.IException {
	summary := []*models.StockItemSummary{
		{StockItemID: stockItemId, Type: models.Sellable},
		{StockItemID: stockItemId, Type: models.NonSellable},
		{StockItemID: stockItemId, Type: models.Backorder},
		{StockItemID: stockItemId, Type: models.Preorder},
	}

	return service.summaryRepo.CreateStockItemSummary(summary)
}

func (service *summaryService) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) exceptions.IException {
	stockItem, err := service.stockItemRepo.GetStockItemById(stockItemId)
	if err != nil {
		return err
	}

	summary, err := service.summaryRepo.GetSummaryItemByType(stockItemId, unitType)
	if err != nil {
		return err
	}

	// changing status from onHand does not affect onHand count, so skip it
	if status.From != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.From, -qty)
	}

	// update count of .To status if it is new record or it's not moved to onHand status
	// or shipped (because in that case, the stock item unit is deleted).
	if status.From == models.StatusEmpty || status.To != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.To, qty)
	}

	summary = updateAfs(summary, status, qty)
	summary = updateAfsCost(summary, stockItem)

	// update stock item summary values
	if err := service.summaryRepo.UpdateStockItemSummary(summary); err != nil {
		log.Printf("Error updating stock_item_summaries with exceptions.IException: %s", err.ToString())
	}

	// create related stock item transaction
	return service.CreateStockItemTransaction(summary, status.To, qty)
}

func (service *summaryService) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) exceptions.IException {
	transaction := &models.StockItemTransaction{
		StockItemId:    summary.StockItemID,
		Type:           summary.Type,
		Status:         status,
		QuantityNew:    getStatusAmountChange(summary, status),
		QuantityChange: qty,
		AFSNew:         uint(summary.AFS),
	}

	if err := service.summaryRepo.CreateStockItemTransaction(transaction); err != nil {
		log.Printf("Error creating stock_item_transactions with exceptions.IException: %s", err.ToString())
	}

	return nil
}

func updateStatusUnitsAmount(summary *models.StockItemSummary, status models.UnitStatus, qty int) *models.StockItemSummary {
	if status == "" {
		return summary
	}

	switch status {
	case models.StatusOnHand:
		summary.OnHand += qty
	case models.StatusOnHold:
		summary.OnHold += qty
	case models.StatusReserved:
		summary.Reserved += qty
	}

	return summary
}

func updateAfs(summary *models.StockItemSummary, shift models.StatusChange, qty int) *models.StockItemSummary {
	if shift.To == models.StatusOnHand {
		summary.AFS += qty
	}

	if shift.From == models.StatusOnHand {
		summary.AFS -= qty
	}

	return summary
}

func updateAfsCost(summary *models.StockItemSummary, stockItem *models.StockItem) *models.StockItemSummary {
	summary.AFSCost = summary.AFS * stockItem.DefaultUnitCost

	return summary
}

func getStatusAmountChange(summary *models.StockItemSummary, status models.UnitStatus) uint {
	field := strings.Title(string(status))
	r := reflect.ValueOf(summary)
	f := reflect.Indirect(r).FieldByName(field)

	return uint(f.Int())
}
