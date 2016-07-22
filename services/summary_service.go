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
	CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error
	UpdateStockItemSummary(stockItemID uint, qty int, status statusChange, dbContext *gorm.DB) error

	GetSummaries() ([]*models.StockItemSummary, error)
	GetSummaryBySKU(sku string) (*models.StockItemSummary, error)
}

func NewSummaryService(db *gorm.DB) ISummaryService {
	return &summaryService{db}
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error {
	db := service.resolveDb(dbContext)

	summary := models.StockItemSummary{
		StockItemID: stockItemId,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := db.Create(&summary).Error; err != nil {
		return err
	}

	return nil
}

func (service *summaryService) UpdateStockItemSummary(stockItemID uint, qty int, status statusChange, dbContext *gorm.DB) error {
	db := service.resolveDb(dbContext)

	summary := &models.StockItemSummary{}
	if err := db.Where("stock_item_id = ?", stockItemID).First(summary).Error; err != nil {
		return err
	}

	summary = updateStatus(summary, status.from, -qty)
	summary = updateStatus(summary, status.to, qty)

	if err := db.Save(summary).Error; err != nil {
		return err
	}

	return nil
}

func (service *summaryService) GetSummaries() ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	if err := service.db.Find(summary).Error; err != nil {
		return nil, err
	}

	return summary, nil
}

func (service *summaryService) GetSummaryBySKU(sku string) (*models.StockItemSummary, error) {
	summary := &models.StockItemSummary{}
	err := service.db.
		Table("stock_item_summaries").
		Joins("left join stock_items si on si.id = stock_item_summaries.stock_item_id").
		Where("si.sku = ?", sku).First(summary).
		Error
	if err != nil {
		return nil, err
	}

	return summary, nil
}

func (service *summaryService) resolveDb(db *gorm.DB) *gorm.DB {
	if db != nil {
		return db
	} else {
		return service.db
	}
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
