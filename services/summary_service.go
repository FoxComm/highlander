package services

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"

	"log"
	"reflect"
	"strings"
)

type summaryService struct {
	summaryRepo   repositories.ISummaryRepository
	stockItemRepo repositories.IStockItemRepository
}

type ISummaryService interface {
	CreateStockItemSummary(stockItemId uint) error
	UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error

	CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error

	GetSummary() ([]*models.StockItemSummary, error)
	GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error)
}

func NewSummaryService(summaryRepo repositories.ISummaryRepository, stockItemRepo repositories.IStockItemRepository) ISummaryService {
	return &summaryService{summaryRepo, stockItemRepo}
}

func (service *summaryService) GetSummary() ([]*models.StockItemSummary, error) {
	return service.summaryRepo.GetSummary()
}

func (service *summaryService) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error) {
	return service.summaryRepo.GetSummaryBySKU(sku)
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint) error {
	summary := []*models.StockItemSummary{
		{StockItemID: stockItemId, Type: models.Sellable},
		{StockItemID: stockItemId, Type: models.NonSellable},
		{StockItemID: stockItemId, Type: models.Backorder},
		{StockItemID: stockItemId, Type: models.Preorder},
	}

	return service.summaryRepo.CreateStockItemSummary(summary)
}

func (service *summaryService) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error {
	stockItem, err := service.stockItemRepo.GetStockItemById(stockItemId)
	if err != nil {
		return err
	}

	summary, err := service.summaryRepo.GetSummaryItemByType(stockItemId, unitType)
	if err != nil {
		fmt.Printf("stockItemId: %d\nunitType: %s\n", stockItemId, unitType)
		return err
	}

	// changing status from onHand does not affect onHand count, so skip it
	if status.From != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.From, -qty)
	}

	// update count of .To status if it is new record or it's not moved to onHand status
	if status.From == models.StatusEmpty || status.To != models.StatusOnHand {
		summary = updateStatusUnitsAmount(summary, status.To, qty)
	}

	summary = updateAfs(summary, status, qty)
	summary = updateAfsCost(summary, stockItem)

	// update stock item summary values
	if err := service.summaryRepo.UpdateStockItemSummary(summary); err != nil {
		log.Printf("Error updating stock_item_summaries with error: %s", err.Error())
	}

	// create related stock item transaction
	return service.CreateStockItemTransaction(summary, status.To, qty)

	//return nil
}

func (service *summaryService) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error {
	transaction := &models.StockItemTransaction{
		StockItemId:    summary.StockItemID,
		Type:           summary.Type,
		Status:         status,
		QuantityNew:    getStatusAmountChange(summary, status),
		QuantityChange: qty,
		AFSNew:         uint(summary.AFS),
	}

	if err := service.summaryRepo.CreateStockItemTransaction(transaction); err != nil {
		log.Printf("Error creating stock_item_transactions with error: %s", err.Error())
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
