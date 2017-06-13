package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorStockItemNotFound = "Stock item with id=%d not found"
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
	GetAFSBySkuId(skuId uint) (*[]models.AfsByType, error)

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

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorStockItemNotFound, id)
		}

		return nil, err
	}

	return si, nil
}

func (repository *stockItemRepository) GetStockItemsBySKUs(skus []string) ([]*models.StockItem, error) {
	items := []*models.StockItem{}
	err := repository.db.Where("sku in (?)", skus).Find(&items).Error

	return items, err
}

func (repository *stockItemRepository) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	afs := &models.AFS{}

	if err := repository.getAFSQuery(unitType).Where("si.id = ?", id).Find(afs).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorStockItemNotFound, id)
		}

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

func (repository *stockItemRepository) GetAFSBySkuId(skuId uint) (*[]models.AfsByType, error) {
	var afs []models.AfsByType

	if err := repository.getAfsWithSkuQuery().Where("sku.id = ?", skuId).Find(&afs).Error; err != nil {
		return nil, err
	}

	if len(afs) == 0 {
		return nil, fmt.Errorf("No AFS data for SKU")
	}

	if len(afs) != models.NumberOfUnitTypes {
		return nil, fmt.Errorf("Invalid number of unit types results. Expected %d, actual %d", models.NumberOfUnitTypes, len(afs))
	}

	return &afs, nil
}

func (repository *stockItemRepository) DeleteStockItem(stockItemId uint) error {
	result := repository.db.Delete(&models.StockItem{}, stockItemId)

	if result.Error != nil {
		return result.Error
	}

	if result.RowsAffected == 0 {
		return fmt.Errorf(ErrorStockItemNotFound, stockItemId)
	}

	return nil
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

func (repository *stockItemRepository) getAfsWithSkuQuery() *gorm.DB {
	return repository.db.
		Table("stock_items si").
		Select("s.afs, s.type").
		Joins("LEFT JOIN stock_item_summaries s ON s.stock_item_id=si.id").
		Joins("JOIN skus sku ON sku.code=si.sku")
}
