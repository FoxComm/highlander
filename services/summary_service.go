package services

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type statusChange struct {
	from string
	to   string
}

type summaryService struct {
	db *gorm.DB
}

type ISummaryService interface {
	CreateStockItemSummary(stockItemId uint) error
	UpdateStockItemSummary(stockItemID uint, qty int, status statusChange) error
}

func NewSummaryService(db *gorm.DB) ISummaryService {
	return &summaryService{db}
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint) error {
	summary := models.StockItemSummary{
		StockItemID: stockItemId,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := service.db.Create(&summary).Error; err != nil {
		return err
	}

	return nil
}

func (service *summaryService) UpdateStockItemSummary(stockItemID uint, qty int, status statusChange) error {
	txn := service.db.Begin()

	summary := &models.StockItemSummary{}
	if err := txn.Where("stock_item_id = ?", stockItemID).First(summary).Error; err != nil {
		txn.Rollback()
		return err
	}

	summary = updateStatus(summary, status.from, -qty)
	summary = updateStatus(summary, status.to, qty)

	if err := txn.Save(summary).Error; err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
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
