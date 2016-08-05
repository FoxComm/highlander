package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type summaryRepository struct {
	db *gorm.DB
}

type ISummaryRepository interface {
	GetSummary() ([]*models.StockItemSummary, error)
	GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error)

	GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, error)

	CreateStockItemSummary(summary []*models.StockItemSummary) error
	UpdateStockItemSummary(summary *models.StockItemSummary) error
}

func NewSummaryRepository(db *gorm.DB) ISummaryRepository {
	return &summaryRepository{db}
}

func (repository *summaryRepository) GetSummary() ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	err := repository.db.
		Table("stock_item_summaries s").
		Select("s.*, si.sku, sl.id as stock_location_id, sl.name as stock_location_name").
		Joins("JOIN stock_items si ON si.id = s.stock_item_id").
		Joins("JOIN stock_locations sl ON si.stock_location_id = sl.id").
		Order("created_at").
		Scan(&summary).
		Error

	return summary, err
}

func (repository *summaryRepository) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	err := repository.db.
		Table("stock_item_summaries s").
		Select("s.*, si.sku, sl.id as stock_location_id, sl.name as stock_location_name").
		Joins("JOIN stock_items si ON si.id = s.stock_item_id").
		Joins("JOIN stock_locations sl ON si.stock_location_id = sl.id").
		Where("si.sku = ?", sku).
		Order("created_at").
		Scan(&summary).
		Error

	if len(summary) == 0 {
		return nil, gorm.ErrRecordNotFound
	}

	return summary, err
}

func (repository *summaryRepository) GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, error) {
	summary := &models.StockItemSummary{}
	result := repository.db.Where("stock_item_id = ? AND type = ?", stockItemId, unitType).First(summary)

	return summary, result.Error
}

func (repository *summaryRepository) CreateStockItemSummary(summary []*models.StockItemSummary) error {
	txn := repository.db.Begin()

	for _, item := range summary {
		if err := txn.Create(item).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (repository *summaryRepository) UpdateStockItemSummary(summary *models.StockItemSummary) error {
	return repository.db.Save(summary).Error
}
