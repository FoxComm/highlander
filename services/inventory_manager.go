package services

import (
	"errors"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/logging"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type InventoryManager struct {
	db     *gorm.DB
	logger logging.Logger
}

// NewInventoryManager creates a new InventoryManager.
func NewInventoryManager() (*InventoryManager, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	im := &InventoryManager{
		logger: logging.Log,
		db:     db,
	}

	return im, nil
}

func (im *InventoryManager) FindStockItemByID(id uint) (*responses.StockItem, error) {
	si := &models.StockItem{}
	if err := im.db.First(si, id).Error; err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(si), nil
	}
}

func (im *InventoryManager) CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	si := models.NewStockItemFromPayload(payload)

	if err := im.db.Create(si).Error; err != nil {
		return nil, err
	}

	summary := models.StockItemSummary{
		StockItemID: si.ID,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}

	if err := im.db.Create(&summary).Error; err != nil {
		return nil, err
	}

	return responses.NewStockItemFromModel(si), nil
}

func (im *InventoryManager) IncrementStockItemUnits(id uint, payload *payloads.IncrementStockItemUnits) error {
	units := models.NewStockItemUnitsFromPayload(id, payload)

	txn := im.db.Begin()

	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	if err := txn.Commit().Error; err != nil {
		return nil
	}

	go im.UpdateStockItem(id, payload.Qty, payload.Status)

	return nil
}

func (im *InventoryManager) DecrementStockItemUnits(id uint, payload *payloads.DecrementStockItemUnits) error {
	txn := im.db.Begin()

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

	go im.UpdateStockItem(id, -1*payload.Qty, "onHand")

	return txn.Commit().Error
}

func (im *InventoryManager) UpdateStockItem(stockItemID uint, qty int, status string) {
	txn := im.db.Begin()

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
