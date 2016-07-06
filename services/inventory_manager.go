package services

import (
	"errors"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
)

func FindStockItemByID(id uint) (*responses.StockItem, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	si := &models.StockItem{}
	if err := db.First(si, id).Error; err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(si), nil
	}
}

func CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	si := models.NewStockItemFromPayload(payload)

	if err := db.Create(si).Error; err != nil {
		return nil, err
	}

	summary := models.StockItemSummary{
		StockItemID: si.ID,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := db.Create(&summary).Error; err != nil {
		return nil, err
	}

	return responses.NewStockItemFromModel(si), nil
}

func IncrementStockItemUnits(id uint, payload *payloads.IncrementStockItemUnits) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	units := models.NewStockItemUnitsFromPayload(id, payload)

	txn := db.Begin()

	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	if err := txn.Commit().Error; err != nil {
		return nil
	}

	go UpdateStockItem(id, payload.Qty, payload.Status)

	return nil
}

func DecrementStockItemUnits(id uint, payload *payloads.DecrementStockItemUnits) error {
	db, err := config.DefaultConnection()
	if err != nil {
		return err
	}

	txn := db.Begin()

	// Check to make sure there are enough on-hand items.
	units := []models.StockItemUnit{}
	err = txn.Limit(payload.Qty).Order("created_at desc").Where("status = ?", "onHand").Find(&units).Error
	if err != nil {
		txn.Rollback()
		return err
	}

	if len(units) < payload.Qty {
		txn.Rollback()
		return errors.New("Not enough onHand units to decrement")
	}

	for _, v := range units {
		if err := txn.Delete(&v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	go UpdateStockItem(id, -1*payload.Qty, "onHand")

	return txn.Commit().Error
}

func UpdateStockItem(stockItemID uint, qty int, status string) {
	db, err := config.DefaultConnection()
	if err != nil {
		return
	}

	txn := db.Begin()

	summary := &models.StockItemSummary{}
	if err := txn.Where("stock_item_id = ?", stockItemID).First(summary).Error; err != nil {
		txn.Rollback()
		return
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
		return
	}

	txn.Commit()
}
