package services

import (
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
)

type statusChange struct {
	from string
	to   string
}

func CreateStockItemSummary(stockItemId uint) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	summary := models.StockItemSummary{
		StockItemID: stockItemId,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := db.Create(&summary).Error; err != nil {
		return err
	}

	return err
}

func UpdateStockItemSummary(stockItemID uint, qty int, status statusChange) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	txn := db.Begin()

	summary := &models.StockItemSummary{}
	if err := txn.Where("stock_item_id = ?", stockItemID).First(summary).Error; err != nil {
		txn.Rollback()
		return err
	}

	summary = updateStatus(summary, status.from, -qty)
	summary = updateStatus(summary, status.to, qty)

	if err := txn.Save(summary).Error; err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
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
