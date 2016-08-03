package services

import (
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type StatusChange struct {
	from string
	to   string
}

type summaryService struct {
	db *gorm.DB
}

type ISummaryService interface {
	CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error
	UpdateStockItemSummary(stockItemId, typeId uint, qty int, status StatusChange, dbContext *gorm.DB) error

	GetSummary() ([]*models.StockItemSummary, error)
	GetSummaryBySKU(sku string) (*models.StockItemSummary, error)
}

func NewSummaryService(db *gorm.DB) ISummaryService {
	return &summaryService{db}
}

func (service *summaryService) GetSummary() ([]*models.StockItemSummary, error) {
	summary := []*models.StockItemSummary{}
	err := service.db.
		Select("stock_item_summaries.*, si.sku").
		Joins("JOIN stock_items si ON si.id = stock_item_summaries.stock_item_id").
		Order("created_at").
		Find(&summary).
		Error

	if err != nil {
		return nil, err
	}

	return summary, nil
}

func (service *summaryService) GetSummaryBySKU(sku string) (*models.StockItemSummary, error) {
	summary := &models.StockItemSummary{}
	err := service.db.
		Select("stock_item_summaries.*, si.sku").
		Joins("JOIN stock_items si ON si.id = stock_item_summaries.stock_item_id").
		Where("si.sku = ?", sku).
		First(summary).
		Error
	if err != nil {
		return nil, err
	}

	return summary, nil
}

func (service *summaryService) CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error {
	db := service.resolveDb(dbContext)
	types := models.StockItemTypes()

	var err error

	err = createStockItemSummary(stockItemId, types.Sellable, db)
	err = createStockItemSummary(stockItemId, types.NonSellable, db)
	err = createStockItemSummary(stockItemId, types.Backorder, db)
	err = createStockItemSummary(stockItemId, types.Preorder, db)

	return err
}

func (service *summaryService) UpdateStockItemSummary(stockItemId, typeId uint, qty int, status StatusChange, dbContext *gorm.DB) error {
	db := service.resolveDb(dbContext)

	stockItem := &models.StockItem{}
	if err := db.First(stockItem, stockItem).Error; err != nil {
		return err
	}

	summary := &models.StockItemSummary{}
	if err := db.Where("stock_item_id = ? AND type_id = ?", stockItemId, typeId).First(summary).Error; err != nil {
		return err
	}

	summary = updateStatus(summary, status.from, -qty)
	summary = updateStatus(summary, status.to, qty)

	summary = updateAfs(summary, status, qty)
	summary = updateAfsCost(summary, stockItem)

	if err := db.Save(summary).Error; err != nil {
		return err
	}

	return nil
}

func (service *summaryService) resolveDb(db *gorm.DB) *gorm.DB {
	if db != nil {
		return db
	} else {
		return service.db
	}
}

func createStockItemSummary(stockItemId uint, typeId uint, db *gorm.DB) error {
	summary := models.StockItemSummary{StockItemID: stockItemId, TypeID: typeId}

	if err := db.Create(&summary).Error; err != nil {
		return err
	}

	return nil
}

func updateStatus(summary *models.StockItemSummary, status string, qty int) *models.StockItemSummary {
	if status == "" {
		return summary
	}

	switch status {
	case "onHand":
		summary.OnHand += qty
	case "onHold":
		summary.OnHold += qty
	case "reserved":
		summary.Reserved += qty
	}

	return summary
}

func updateAfs(summary *models.StockItemSummary, shift StatusChange, qty int) *models.StockItemSummary {
	if shift.to == "onHand" {
		summary.AFS += qty
	}

	if shift.from == "onHand" {
		summary.AFS -= qty
	}

	return summary
}

func updateAfsCost(summary *models.StockItemSummary, stockItem *models.StockItem) *models.StockItemSummary {
	summary.AFSCost = summary.AFS * stockItem.DefaultUnitCost

	return summary
}
