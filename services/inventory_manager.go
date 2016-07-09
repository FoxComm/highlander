package services

import (
	"errors"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type InventoryManager struct {
	db *gorm.DB
}

func MakeInventoryManager() (mgr InventoryManager, err error) {
	mgr = InventoryManager{}
	mgr.db, err = config.DefaultConnection()
	return
}

func (mgr InventoryManager) FindStockItemByID(id uint) (*responses.StockItem, error) {
	si := &models.StockItem{}
	if err := mgr.db.First(si, id).Error; err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(si), nil
	}
}

func (mgr InventoryManager) CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	si := models.NewStockItemFromPayload(payload)

	if err := mgr.db.Create(si).Error; err != nil {
		return nil, err
	}

	summary := models.StockItemSummary{
		StockItemID: si.ID,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := mgr.db.Create(&summary).Error; err != nil {
		return nil, err
	}

	return responses.NewStockItemFromModel(si), nil
}

func (mgr InventoryManager) IncrementStockItemUnits(id uint, payload *payloads.IncrementStockItemUnits) error {
	units := models.NewStockItemUnitsFromPayload(id, payload)

	txn := mgr.db.Begin()

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

func (mgr InventoryManager) DecrementStockItemUnits(id uint, payload *payloads.DecrementStockItemUnits) error {
	txn := mgr.db.Begin()

	// Check to make sure there are enough on-hand items.
	units := []models.StockItemUnit{}
	err := txn.Limit(payload.Qty).Order("created_at desc").Where("status = ?", "onHand").Find(&units).Error
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
