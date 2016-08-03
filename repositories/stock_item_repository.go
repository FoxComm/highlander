package repositories

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type stockItemRepository struct {
	db *gorm.DB
}

type IStockItemRepository interface {
	GetStockItems() ([]*models.StockItem, error)
	GetStockItemById(id uint) (*models.StockItem, error)

	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)
	DeleteStockItem(stockItemId uint) error
}

func NewStockItemRepository(db *gorm.DB) IStockItemRepository {
	return &stockItemRepository{db}
}

func (repository *stockItemRepository) resolveDb(db *gorm.DB) *gorm.DB {
	if db != nil {
		return db
	} else {
		return repository.db
	}
}

func (repository *stockItemRepository) GetStockItems() ([]*models.StockItem, error) {
	items := []*models.StockItem{}
	if err := repository.db.Find(&items).Error; err != nil {
		return nil, err
	}

	return items, nil
}

func (repository *stockItemRepository) GetStockItemById(id uint) (*models.StockItem, error) {
	si := &models.StockItem{}
	if err := repository.db.First(si, id).Error; err != nil {
		return nil, err
	}

	return si, nil
}

func (repository *stockItemRepository) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	err := repository.db.Create(stockItem).Error

	return stockItem, err
}

func (repository *stockItemRepository) DeleteStockItem(stockItemId uint) error {
	return repository.db.Delete(&models.StockItem{}, stockItemId).Error
}
