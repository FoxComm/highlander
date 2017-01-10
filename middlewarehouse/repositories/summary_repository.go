package repositories

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/logging"
	"github.com/FoxComm/highlander/middlewarehouse/common/transaction"
	"github.com/jinzhu/gorm"
)

const (
	ErrorSummaryNotFound              = "Summary with id=%d not found"
	ErrorSummaryForSKUNotFound        = "Summary for sku=%d not found"
	ErrorSummaryForItemByTypeNotFound = "Summary for stock item with id=%d and type=%s not found"
)

type summaryRepository struct {
	db *gorm.DB
}

type ISummaryRepository interface {
	WithTransaction(txn *gorm.DB) ISummaryRepository

	GetSummary() ([]*models.StockItemSummary, error)
	GetSummaryBySkuID(skuID uint) ([]*models.StockItemSummary, error)

	GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, error)

	CreateStockItemSummary(summary []*models.StockItemSummary) error
	UpdateStockItemSummary(summary *models.StockItemSummary) error

	CreateStockItemTransaction(transaction *models.StockItemTransaction) error
}

func NewSummaryRepository(db *gorm.DB) ISummaryRepository {
	return &summaryRepository{db}
}

// WithTransaction returns a shallow copy of repository with its db changed to txn. The provided txn must be non-nil.
func (repository *summaryRepository) WithTransaction(txn *gorm.DB) ISummaryRepository {
	if txn == nil {
		panic("nil transaction")
	}

	return NewSummaryRepository(txn)
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

func (repository *summaryRepository) GetSummaryBySkuID(skuID uint) ([]*models.StockItemSummary, error) {
	summaries := []*models.StockItemSummary{}
	err := repository.db.
		Preload("StockItem").
		Preload("StockItem.StockLocation").
		Joins("left join stock_items si ON stock_item_summaries.stock_item_id=si.id").
		Where("si.sku_id = ?", skuID).
		Order("created_at").
		Find(&summaries).
		Error

	if len(summaries) == 0 {
		return nil, fmt.Errorf(ErrorSummaryForSKUNotFound, skuID)
	}

	for _, summary := range summaries {
		logSummary(summary, "GetSummaryBySkuID: Retrieved summary")
	}
	return summaries, err
}

func (repository *summaryRepository) GetSummaryItemByType(stockItemId uint, unitType models.UnitType) (*models.StockItemSummary, error) {
	summary := &models.StockItemSummary{}
	result := repository.db.Where("stock_item_id = ? AND type = ?", stockItemId, unitType).First(summary)

	if result.Error != nil {
		if result.Error == gorm.ErrRecordNotFound {
			return nil, fmt.Errorf(ErrorSummaryForItemByTypeNotFound, stockItemId, unitType)
		}

		return nil, result.Error
	}

	return summary, nil
}

func (repository *summaryRepository) CreateStockItemSummary(summary []*models.StockItemSummary) error {
	txn := transaction.NewTransaction(repository.db).Begin()

	for _, item := range summary {
		// use `UPDATE SET stock_item_id = '%d'` as postgres driver does not return anything on `DO NOTHING`
		// resulting in 'no rows in result set' sql error
		onConflict := fmt.Sprintf(
			"ON CONFLICT (stock_item_id, type) DO UPDATE SET stock_item_id = '%d'",
			item.StockItemID,
		)

		if err := txn.Set("gorm:insert_option", onConflict).Create(item).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (repository *summaryRepository) UpdateStockItemSummary(summary *models.StockItemSummary) error {
	logSummary(summary, "UpdatingStockItemSummary: Before updating the summary")
	err := repository.db.Save(summary).Error

	if err == gorm.ErrRecordNotFound {
		return fmt.Errorf(ErrorSummaryNotFound, summary.ID)
	}

	logSummary(summary, "UpdatingStockItemSummary: After updating the summary")
	return err
}

func (repository *summaryRepository) CreateStockItemTransaction(transaction *models.StockItemTransaction) error {
	return repository.db.Create(transaction).Error
	// return nil
}

func logSummary(summary *models.StockItemSummary, message string) {
	logging.Log.Debugf(message, logging.M{
		"ID":       summary.ID,
		"Type":     summary.Type,
		"OnHand":   summary.OnHand,
		"OnHold":   summary.OnHold,
		"Reserved": summary.Reserved,
		"Shipped":  summary.Shipped,
	})
}
