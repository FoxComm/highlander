package repositories

import (
	"fmt"

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
	GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error)
	GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error)

	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)
	UpsertStockItem(item *models.StockItem) error
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

func (repository *stockItemRepository) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	afs := &models.AFS{}

	if err := repository.getAFSQuery(unitType).Where("si.id = ?", id).Find(afs).Error; err != nil {
		return nil, err
	}

	return afs, nil
}

func (repository *stockItemRepository) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	afs := &models.AFS{}

	if err := repository.getAFSQuery(unitType).Where("si.sku = ?", sku).Find(afs).Error; err != nil {
		return nil, err
	}

	return afs, nil
}

func (repository *stockItemRepository) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	if err := repository.db.Create(stockItem).Error; err != nil {
		return nil, err
	}

	return repository.GetStockItemById(stockItem.ID)
}

func (repository *stockItemRepository) DeleteStockItem(stockItemId uint) error {
	return repository.db.Delete(&models.StockItem{}, stockItemId).Error
}

func (repository *stockItemRepository) UpsertStockItem(item *models.StockItem) error {
	onConflict := fmt.Sprintf(
		"ON CONFLICT (sku, stock_location_id) DO UPDATE SET default_unit_cost = '%d'",
		item.DefaultUnitCost,
	)

	if err := repository.db.Set("gorm:insert_option", onConflict).Create(item).Error; err != nil {
		return err
	}

	return nil
}

func (repository *stockItemRepository) getAFSQuery(unitType models.UnitType) *gorm.DB {
	return repository.db.
		Table("stock_items si").
		Select("si.id as stock_item_id, si.sku, s.afs").
		Joins("left join stock_item_summaries s ON s.stock_item_id=si.id").
		Where("s.type = ?", unitType)
}
