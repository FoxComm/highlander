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
	GetStockItemsBySKUs(skus []string) ([]*models.StockItem, error)

	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)
	DeleteStockItem(stockItemId uint) error
}

func NewStockItemRepository(db *gorm.DB) IStockItemRepository {
	return &stockItemRepository{db}
}

func (repository *stockItemRepository) GetStockItems() ([]*models.StockItem, error) {
	items := []*models.StockItem{}
	err := repository.db.Find(&items).Error

	return items, err
}

func (repository *stockItemRepository) GetStockItemById(id uint) (*models.StockItem, error) {
	si := &models.StockItem{}
	err := repository.db.First(si, id).Error

	return si, err
}

func (repository *stockItemRepository) GetStockItemsBySKUs(skus []string) ([]*models.StockItem, error) {
	items := []*models.StockItem{}
	err := repository.db.Where("sku in (?)", skus).Find(&items).Error

	return items, err
}

func (repository *stockItemRepository) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	err := repository.db.Create(stockItem).Error

	return stockItem, err
}

func (repository *stockItemRepository) DeleteStockItem(stockItemId uint) error {
	return repository.db.Delete(&models.StockItem{}, stockItemId).Error
}
