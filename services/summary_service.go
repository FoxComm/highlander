package services

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/FoxComm/middlewarehouse/repositories"
)

type summaryService struct {
	summaryRepo   repositories.ISummaryRepository
	stockItemRepo repositories.IStockItemRepository
}

type ISummaryService interface {
	CreateStockItemSummary(stockItemId uint) error
	UpdateStockItemSummary(stockItemId, typeId uint, qty int, status models.StatusChange) error

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
		{StockItemID: stockItemId, TypeID: models.Sellable},
		{StockItemID: stockItemId, TypeID: models.NonSellable},
		{StockItemID: stockItemId, TypeID: models.Backorder},
		{StockItemID: stockItemId, TypeID: models.Preorder},
	}

	return service.summaryRepo.CreateStockItemSummary(summary)
}

func (service *summaryService) UpdateStockItemSummary(stockItemId, typeId uint, qty int, status models.StatusChange) error {
	stockItem, err := service.stockItemRepo.GetStockItemById(stockItemId)
	if err != nil {
		return err
	}

	summary, err := service.summaryRepo.GetSummaryItemByType(stockItemId, typeId)
	if err != nil {
		return err
	}

	if status.From != "onHand" {
		summary = updateStatus(summary, status.From, -qty)
	}
	if status.From == "" || status.To != "onHand" {
		summary = updateStatus(summary, status.To, qty)
	}

	summary = updateAfs(summary, status, qty)
	summary = updateAfsCost(summary, stockItem)

	return service.summaryRepo.UpdateStockItemSummary(summary)
}

func updateStatus(summary *models.StockItemSummary, status string, qty int) *models.StockItemSummary {
	if status == "" {
		return summary
	}

	switch status {
	case "onHand":
		summary.OnHand += qty
	case "onHold":
		summary.OnHold += qty
	case "reserved":
		summary.Reserved += qty
	}

	return summary
}

func updateAfs(summary *models.StockItemSummary, shift models.StatusChange, qty int) *models.StockItemSummary {
	if shift.To == "onHand" {
		summary.AFS += qty
	}

	if shift.From == "onHand" {
		summary.AFS -= qty
	}

	return summary
}

func updateAfsCost(summary *models.StockItemSummary, stockItem *models.StockItem) *models.StockItemSummary {
	summary.AFSCost = summary.AFS * stockItem.DefaultUnitCost

	return summary
}
