package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/jinzhu/gorm"
)

const (
	ErrorSummaryNotFound              = "Summary with id=%d not found"
	ErrorSummaryForSKUNotFound        = "Summary for sku=%s not found"
	ErrorSummaryForItemByTypeNotFound = "Summary for stock item with id=%d and type=%s not found"
	summaryEntity                     = "summary"
)

type summaryRepository struct {
	db *gorm.DB
}

type ISummaryRepository interface {
	GetSummary() ([]*models.StockItemSummary, exceptions.IException)
	GetSummaryBySKU(sku string) ([]*models.StockItemSummary, exceptions.IException)

	GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, exceptions.IException)

	CreateStockItemSummary(summary []*models.StockItemSummary) exceptions.IException
	UpdateStockItemSummary(summary *models.StockItemSummary) exceptions.IException

	CreateStockItemTransaction(transaction *models.StockItemTransaction) exceptions.IException
}

func NewSummaryRepository(db *gorm.DB) ISummaryRepository {
	return &summaryRepository{db}
}

func (repository *summaryRepository) GetSummary() ([]*models.StockItemSummary, exceptions.IException) {
	summary := []*models.StockItemSummary{}
	err := repository.db.
		Preload("StockItem").
		Preload("StockItem.StockLocation").
		Order("created_at").
		Find(&summary).
		Error

	return summary, NewDatabaseException(err)
}

func (repository *summaryRepository) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, exceptions.IException) {
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
		return nil, NewSummaryForSKUNotFoundException(sku, fmt.Errorf(ErrorSummaryForSKUNotFound, sku))
	}

	return summary, NewDatabaseException(err)
}

func (repository *summaryRepository) GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, exceptions.IException) {
	summary := &models.StockItemSummary{}
	result := repository.db.Where("stock_item_id = ? AND type = ?", stockItemId, unitType).First(summary)

	if result.Error != nil {
		if result.Error == gorm.ErrRecordNotFound {
			return nil, NewSummaryForItemByTypeNotFoundException(stockItemId, unitType, fmt.Errorf(ErrorSummaryForItemByTypeNotFound, stockItemId, unitType))
		}

		return nil, NewDatabaseException(result.Error)
	}

	return summary, nil
}

func (repository *summaryRepository) CreateStockItemSummary(summary []*models.StockItemSummary) exceptions.IException {
	txn := repository.db.Begin()

	for _, item := range summary {
		// use `UPDATE SET stock_item_id = '%d'` as postgres driver does not return anything on `DO NOTHING`
		// resulting in 'no rows in result set' sql exceptions.IException
		onConflict := fmt.Sprintf(
			"ON CONFLICT (stock_item_id, type) DO UPDATE SET stock_item_id = '%d'",
			item.StockItemID,
		)

		if err := txn.Set("gorm:insert_option", onConflict).Create(item).Error; err != nil {
			txn.Rollback()
			return NewDatabaseException(err)
		}
	}

	return NewDatabaseException(txn.Commit().Error)
}

func (repository *summaryRepository) UpdateStockItemSummary(summary *models.StockItemSummary) exceptions.IException {
	err := repository.db.Save(summary).Error

	if err == gorm.ErrRecordNotFound {
		return NewEntityNotFoundException(summaryEntity, string(summary.ID), fmt.Errorf(ErrorSummaryNotFound, summary.ID))
	}

	return NewDatabaseException(err)
}

func (repository *summaryRepository) CreateStockItemTransaction(transaction *models.StockItemTransaction) exceptions.IException {
	return NewDatabaseException(repository.db.Create(transaction).Error)
}

type summaryForSkuNotFoundException struct {
	cls string `json:"type"`
	sku string
	exceptions.Exception
}

func NewSummaryForSKUNotFoundException(sku string, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return summaryForSkuNotFoundException{
		cls:       "summaryForSkuNotFound",
		sku:       sku,
		Exception: exceptions.Exception{error},
	}
}

type summaryForItemByTypeNotFoundException struct {
	cls      string `json:"type"`
	item     uint
	unitType models.UnitType
	exceptions.Exception
}

func NewSummaryForItemByTypeNotFoundException(item uint, unitType models.UnitType, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return summaryForItemByTypeNotFoundException{
		cls:       "summaryForSkuNotFound",
		item:      item,
		unitType:  unitType,
		Exception: exceptions.Exception{error},
	}
}
