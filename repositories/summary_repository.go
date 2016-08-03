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

	GetSummaryItemByType(stockItemId uint, typeId uint) (*models.StockItemSummary, error)

	CreateStockItemSummary(stockItemId uint, typeId uint, dbContext *gorm.DB) error
	UpdateStockItemSummary(summary *models.StockItemSummary, dbContext *gorm.DB) error
}

func NewSummaryRepository(db *gorm.DB) ISummaryRepository {
	return &summaryRepository{db}
}

func (repository *summaryRepository) resolveDb(db *gorm.DB) *gorm.DB {
	if db != nil {
		return db
	} else {
		return repository.db
	}
}

func (repository *summaryRepository) GetSummary() ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	err := repository.db.
		Debug().
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
	err := repository.db.Debug().
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

func (repository *summaryRepository) GetSummaryItemByType(stockItemId uint, typeId uint) (*models.StockItemSummary, error) {
	summary := &models.StockItemSummary{}
	result := repository.db.Where("stock_item_id = ? AND type_id = ?", stockItemId, typeId).First(summary)

	return summary, result.Error
}

func (repository *summaryRepository) CreateStockItemSummary(stockItemId uint, typeId uint, dbContext *gorm.DB) error {
	db := repository.resolveDb(dbContext)
	summary := models.StockItemSummary{StockItemID: stockItemId, TypeID: typeId}

	return db.Create(&summary).Error
}

func (repository *summaryRepository) UpdateStockItemSummary(summary *models.StockItemSummary, dbContext *gorm.DB) error {
	db := repository.resolveDb(dbContext)

	return db.Save(summary).Error
}
