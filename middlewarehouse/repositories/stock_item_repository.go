package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/jinzhu/gorm"
	"strconv"
)

const (
	ErrorStockItemNotFound = "Stock item with id=%d not found"
	ErrorAFSNotFoundByID = "AFS with id=%d not found"
	ErrorAFSNotFoundBySKU = "AFS for sku=%s not found"
	StockItemEntity        = "stockItem"
)

type stockItemRepository struct {
	db *gorm.DB
}

type IStockItemRepository interface {
	GetStockItems() ([]*models.StockItem, exceptions.IException)
	GetStockItemById(id uint) (*models.StockItem, exceptions.IException)
	GetStockItemsBySKUs(skus []string) ([]*models.StockItem, exceptions.IException)
	GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, exceptions.IException)
	GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, exceptions.IException)

	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, exceptions.IException)
	UpsertStockItem(item *models.StockItem) exceptions.IException
	DeleteStockItem(stockItemId uint) exceptions.IException
}

func NewStockItemRepository(db *gorm.DB) IStockItemRepository {
	return &stockItemRepository{db}
}

func (repository *stockItemRepository) GetStockItems() ([]*models.StockItem, exceptions.IException) {
	items := []*models.StockItem{}
	err := repository.db.Find(&items).Error

	return items, NewDatabaseException(err)
}

func (repository *stockItemRepository) GetStockItemById(id uint) (*models.StockItem, exceptions.IException) {
	si := &models.StockItem{}
	err := repository.db.First(si, id).Error

	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewEntityNotFoundException(StockItemEntity, strconv.Itoa(int(id)), fmt.Errorf(ErrorStockItemNotFound, id))
		}

		return nil, NewDatabaseException(err)
	}

	return si, nil
}

func (repository *stockItemRepository) GetStockItemsBySKUs(skus []string) ([]*models.StockItem, exceptions.IException) {
	items := []*models.StockItem{}
	err := repository.db.Where("sku in (?)", skus).Find(&items).Error

	return items, NewDatabaseException(err)
}

func (repository *stockItemRepository) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	afs := &models.AFS{}

	if err := repository.getAFSQuery(unitType).Where("si.id = ?", id).Find(afs).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewAFSNotFoundByIDException(id, fmt.Errorf(ErrorAFSNotFoundByID, id))
		}

		return nil, NewDatabaseException(err)
	}

	return afs, nil
}

func (repository *stockItemRepository) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	afs := &models.AFS{}

	if err := repository.getAFSQuery(unitType).Where("si.sku = ?", sku).Find(afs).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, NewAFSNotFoundBySKUException(sku, fmt.Errorf(ErrorAFSNotFoundBySKU, sku))
		}

		return nil, NewDatabaseException(err)
	}

	return afs, nil
}

func (repository *stockItemRepository) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, exceptions.IException) {
	if err := repository.db.Create(stockItem).Error; err != nil {
		return nil, NewDatabaseException(err)
	}

	return repository.GetStockItemById(stockItem.ID)
}

func (repository *stockItemRepository) DeleteStockItem(stockItemId uint) exceptions.IException {
	result := repository.db.Delete(&models.StockItem{}, stockItemId)

	if result.Error != nil {
		return NewDatabaseException(result.Error)
	}

	if result.RowsAffected == 0 {
		return NewEntityNotFoundException(StockItemEntity, strconv.Itoa(int(stockItemId)), fmt.Errorf(ErrorStockItemNotFound, stockItemId))
	}

	return nil
}

func (repository *stockItemRepository) UpsertStockItem(item *models.StockItem) exceptions.IException {
	onConflict := fmt.Sprintf(
		"ON CONFLICT (sku, stock_location_id) DO UPDATE SET default_unit_cost = '%d'",
		item.DefaultUnitCost,
	)

	if err := repository.db.Set("gorm:insert_option", onConflict).Create(item).Error; err != nil {
		return NewDatabaseException(err)
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

type AFSNotFoundByIDException struct {
	cls string `json:"type"`
	id  uint
	exceptions.Exception
}

func NewAFSNotFoundByIDException(id uint, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return AFSNotFoundByIDException{
		cls:       "afsNotFoundByID",
		id:        id,
		Exception: exceptions.Exception{error},
	}
}

type AFSNotFoundBySKUException struct {
	cls string `json:"type"`
	sku string
	exceptions.Exception
}

func NewAFSNotFoundBySKUException(sku string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return AFSNotFoundBySKUException{
		cls:       "afsNotFoundBySKU",
		sku:       sku,
		Exception: exceptions.Exception{error},
	}
}
