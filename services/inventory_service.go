package services

import (
	"database/sql"
	"errors"
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type inventoryService struct {
	db             *gorm.DB
	summaryService ISummaryService
}

type IInventoryService interface {
	GetStockItems() ([]*models.StockItem, error)
	GetStockItemByID(id uint) (*models.StockItem, error)
	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)

	IncrementStockItemUnits(id uint, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id uint, qty int) error

	ReserveItems(refNum string, skus map[string]int) error
	ReleaseItems(refNum string) error
}

func NewInventoryService(db *gorm.DB, summaryService ISummaryService) IInventoryService {
	return &inventoryService{db, summaryService}
}

func (service *inventoryService) GetStockItems() ([]*models.StockItem, error) {
	items := []*models.StockItem{}
	if err := service.db.Find(&items).Error; err != nil {
		return nil, err
	}

	return items, nil
}

func (service *inventoryService) GetStockItemByID(id uint) (*models.StockItem, error) {
	si := &models.StockItem{}
	if err := service.db.First(si, id).Error; err != nil {
		return nil, err
	}

	return si, nil
}

func (service *inventoryService) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	txn := service.db.Begin()
	if err := txn.Create(stockItem).Error; err != nil {
		txn.Rollback()
		return nil, err
	}

	if err := service.summaryService.CreateStockItemSummary(stockItem.ID, txn); err != nil {
		txn.Rollback()
		return nil, err
	}

	return stockItem, txn.Commit().Error
}

func (service *inventoryService) IncrementStockItemUnits(id uint, units []*models.StockItemUnit) error {
	txn := service.db.Begin()

	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	err := service.summaryService.UpdateStockItemSummary(id, len(units), statusChange{to: "onHand"}, txn)
	if err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
}

func (service *inventoryService) DecrementStockItemUnits(id uint, qty int) error {
	txn := service.db.Begin()

	units, err := onHandStockItemUnits(txn, id, qty)
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

	err = service.summaryService.UpdateStockItemSummary(id, -1*qty, statusChange{to: "onHand"}, txn)
	if err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
}

func (service *inventoryService) ReserveItems(refNum string, skus map[string]int) error {
	txn := service.db.Begin()

	skusList := []string{}
	for code := range skus {
		skusList = append(skusList, code)
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
		units, err := onHandStockItemUnits(txn, si.ID, skus[si.SKU])
		if err != nil {
			txn.Rollback()
			return err
		}

		for _, unit := range units {
			unitsIds = append(unitsIds, unit.ID)
		}

		stockItemsMap[si.ID] = skus[si.SKU]
	}

	// update StockItemUnit.ReservationID field
	updateWith := models.StockItemUnit{
		RefNum: sql.NullString{String: refNum, Valid: true},
		Status: "onHold",
	}

	err := txn.Model(models.StockItemUnit{}).Where("id in (?)", unitsIds).Updates(updateWith).Error
	if err != nil {
		txn.Rollback()
		return err
	}

	for id, qty := range stockItemsMap {
		err := service.summaryService.
			UpdateStockItemSummary(id, qty, statusChange{from: "onHand", to: "onHold"}, txn)
		if err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (service *inventoryService) ReleaseItems(refNum string) error {
	txn := service.db.Begin()

	unitsCount := 0
	txn.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Count(&unitsCount)

	if unitsCount == 0 {
		txn.Rollback()
		return fmt.Errorf("No stock item units associated with %s", refNum)
	}

	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"ref_num": sql.NullString{String: "", Valid: false},
		"status":  "onHand",
	}

	// extract summary update logic
	units := []models.StockItemUnit{}
	if err := txn.Where("ref_num = ?", refNum).Find(&units).Error; err != nil {
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

	err := txn.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Updates(updateWith).Error
	if err != nil {
		txn.Rollback()
		return err
	}

	for id, qty := range stockItemsMap {
		err := service.summaryService.
			UpdateStockItemSummary(id, qty, statusChange{from: "onHold", to: "onHand"}, txn)
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
