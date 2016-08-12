package repositories

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"

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
		Preload("StockItem").
		Preload("StockItem.StockLocation").
		Order("created_at").
		Find(&summary).
		Error

	return summary, err
}

func (repository *summaryRepository) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	err := repository.db.
		Preload("StockItem").
		Preload("StockItem.StockLocation").
		Joins("left join stock_items si ON stock_item_summaries.stock_item_id=si.id").
		Where("si.sku = ?", sku).
		Order("created_at").
		Find(&summary).
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
