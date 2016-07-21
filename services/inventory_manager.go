package services

import (
	"errors"
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
	}

	return responses.NewStockItemFromModel(si), nil
}

func (mgr InventoryManager) CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	si := models.NewStockItemFromPayload(payload)

	if err := mgr.db.Create(si).Error; err != nil {
		return nil, err
	}

	go CreateStockItemSummary(si.ID)

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
		return err
	}

	go UpdateStockItemSummary(id, payload.Qty, statusChange{to: payload.Status})

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

	if err := txn.Commit().Error; err != nil {
		return err
	}

	go UpdateStockItemSummary(id, -1 * payload.Qty, statusChange{to: "onHand"})

	return nil
}

func (mgr InventoryManager) ReserveItems(payload payloads.Reservation) error {
	txn := mgr.db.Begin()

	skusList := []string{}
	skusMap := map[string]int{}
	for _, sku := range payload.SKUs {
		skusList = append(skusList, sku.SKU)
		skusMap[sku.SKU] = int(sku.Qty)
	}

	items := []models.StockItem{}
	if err := txn.Where("sku in (?)", skusList).Find(&items).Error; err != nil {
		return err
	}

	if len(skusList) != len(items) {
		txn.Rollback()
		return errors.New("Wrong SKUs list")
	}

	stockItemsMap := map[uint]int{}
	unitsIds := []uint{}
	for _, si := range items {
		units, err := onHandStockItemUnits(txn, si.ID, skusMap[si.SKU])
		if err != nil {
			txn.Rollback()
			return err
		}

		for _, unit := range units {
			unitsIds = append(unitsIds, unit.ID)
		}

		stockItemsMap[si.ID] = skusMap[si.SKU]
	}

	// update StockItemUnit.ReservationID field
	updateWith := models.StockItemUnit{
		RefNum: sql.NullString{String: payload.RefNum, Valid: true},
		Status: "onHold",
	}

	if err := txn.Model(models.StockItemUnit{}).Where("id in (?)", unitsIds).Updates(updateWith).Error; err != nil {
		txn.Rollback()
		return err
	}

	if err := txn.Commit().Error; err != nil {
		return err
	}

	for id, qty := range stockItemsMap {
		go UpdateStockItemSummary(id, qty, statusChange{from: "onHand", to: "onHold"})
	}

	return nil
}

func (mgr InventoryManager) ReleaseItems(payload payloads.Release) error {
	txn := mgr.db.Begin()

	unitsCount := 0
	txn.Model(&models.StockItemUnit{}).Where("ref_num = ?", payload.RefNum).Count(&unitsCount)

	if unitsCount == 0 {
		txn.Rollback()
		return fmt.Errorf("No stock item units associated with %s", payload.RefNum)
	}

	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"ref_num": sql.NullString{String: "", Valid: false},
		"status": "onHand",
	}

	// extract summary update logic
	units := []models.StockItemUnit{}
	if err := txn.Where("ref_num = ?", payload.RefNum).Find(&units).Error; err != nil {
		return err
	}

	stockItemsMap := map[uint]int{}
	for _, unit := range units {
		if _, ok := stockItemsMap[unit.StockItemID]; ok {
			stockItemsMap[unit.StockItemID] += 1
		} else {
			stockItemsMap[unit.StockItemID] = 1
		}
	}

	err := txn.Model(&models.StockItemUnit{}).Where("ref_num = ?", payload.RefNum).Updates(updateWith).Error
	if err != nil {
		txn.Rollback()
		return err
	}

	if err := txn.Commit().Error; err != nil {
		return err
	}

	for id, qty := range stockItemsMap {
		go UpdateStockItemSummary(id, qty, statusChange{from: "onHold", to: "onHand"})
	}

	return nil
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