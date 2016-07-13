package services

import (
	"database/sql"
	"fmt"

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

	units, err := onHandStockItemUnits(txn, id, payload.Qty)
	if err != nil {
		txn.Rollback()
		return err
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

func (mgr InventoryManager) ReserveItems(payload payloads.Reservation) error {
	if err := payload.Validate(); err != nil {
		return err
	}

	txn := mgr.db.Begin()

	reservation := models.MakeReservationFromPayload(payload)
	if err := txn.Create(&reservation).Error; err != nil {
		txn.Rollback()
		return err
	}

	// TODO: Optimize this a bit.
	siQtyMap := make(map[uint]int)
	unitsIds := []uint{}
	for _, sku := range payload.SKUs {
		item := models.StockItem{}
		if err := txn.Where("sku = ?", sku.SKU).First(&item).Error; err != nil {
			txn.Rollback()
			return err
		}

		units, err := onHandStockItemUnits(txn, item.ID, int(sku.Qty))
		if err != nil {
			txn.Rollback()
			return err
		}

		for _, unit := range units {
			unitsIds = append(unitsIds, unit.ID)
		}

		siQtyMap[item.ID] = int(sku.Qty)
	}


	// update StockItemUnit.ReservationID field
	unitsToUpdate := models.StockItemUnit{
		ReservationID: sql.NullInt64{Int64: int64(reservation.ID), Valid: true},
		Status:        "onHold",
	}

	err := txn.Model(models.StockItemUnit{}).Where("id in (?)", unitsIds).Updates(unitsToUpdate).Error
	if err != nil {
		txn.Rollback()
		return err
	}

	// increment StockItemSummary.Reserved field
	for id, qty := range siQtyMap {
		err := txn.Model(models.StockItemSummary{}).
			Where("stock_item_id = ?", id).
			Update("reserved", gorm.Expr("reserved + ?", qty)).Error

		if err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func onHandStockItemUnits(db *gorm.DB, stockItemID uint, count int) ([]models.StockItemUnit, error) {
	units := []models.StockItemUnit{}
	err := db.Limit(count).
		Order("created_at desc").
		Where("stock_item_id = ?", stockItemID).
		Where("status = ?", "onHand").
		Find(&units).
		Error

	if err != nil {
		return units, err
	}

	if len(units) < count {
		err := fmt.Errorf(
			"Not enough onHand units for stock item %v. Expected %v, got %v",
			stockItemID,
			count,
			len(units),
		)

		return units, err
	}

	return units, nil
}
