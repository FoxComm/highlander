package services

import (
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
)

func UpdateStockItem(stockItemID uint, qty int, status string) error {
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

	switch status {
	case "onHand":
		summary.OnHand += qty
		break
	case "onHold":
		summary.OnHold += qty
		break
	case "reserved":
		summary.Reserved += qty
		break
	}

	if err := txn.Save(summary).Error; err != nil {
		txn.Rollback()
		return err
	}

	txn.Commit()
	return nil
}
