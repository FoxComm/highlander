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
	GetStockItemById(id uint) (*models.StockItem, error)
	CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error)

	IncrementStockItemUnits(id, typeId uint, units []*models.StockItemUnit) error
	DecrementStockItemUnits(id, typeId uint, qty int) error

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

func (service *inventoryService) GetStockItemById(id uint) (*models.StockItem, error) {
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

func (service *inventoryService) IncrementStockItemUnits(stockItemId, typeId uint, units []*models.StockItemUnit) error {
	txn := service.db.Begin()

	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	err := service.summaryService.UpdateStockItemSummary(stockItemId, typeId, len(units), StatusChange{to: "onHand"}, txn)
	if err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
}

func (service *inventoryService) DecrementStockItemUnits(stockItemId, typeId uint, qty int) error {
	txn := service.db.Begin()

	unitsIds, err := onHandStockItemUnits(stockItemId, typeId, qty, txn)
	if err != nil {
		txn.Rollback()
		return err
	}

	if err := txn.Delete(models.StockItemUnit{}, "id in (?)", unitsIds).Error; err != nil {
		txn.Rollback()
		return err
	}

	err = service.summaryService.UpdateStockItemSummary(stockItemId, typeId, -1*qty, StatusChange{to: "onHand"}, txn)
	if err != nil {
		txn.Rollback()
		return err
	}

	return txn.Commit().Error
}

func (service *inventoryService) ReserveItems(refNum string, skus map[string]int) error {
	var err error
	txn := service.db.Begin()

	skusList := []string{}
	for code := range skus {
		skusList = append(skusList, code)
	}

	// stock items associated with SKUs
	items := []models.StockItem{}
	if err = txn.Where("sku in (?)", skusList).Find(&items).Error; err != nil {
		txn.Rollback()
		return err
	}

	if len(skusList) != len(items) {
		txn.Rollback()
		return errors.New("Wrong SKUs list")
	}

	typeId := models.StockItemTypes().Sellable
	stockItemsMap := map[uint]int{}
	unitsIds := []uint{}
	for _, si := range items {
		ids, err := onHandStockItemUnits(si.ID, typeId, skus[si.SKU], txn)
		if err != nil {
			txn.Rollback()
			return err
		}

		unitsIds = append(unitsIds, ids...)
		stockItemsMap[si.ID] = skus[si.SKU]
	}

	updateWith := models.StockItemUnit{
		RefNum: sql.NullString{String: refNum, Valid: true},
		Status: "onHold",
	}

	if err = txn.Model(models.StockItemUnit{}).Where("id in (?)", unitsIds).Updates(updateWith).Error; err != nil {
		txn.Rollback()
		return err
	}

	for id, qty := range stockItemsMap {
		err = service.summaryService.
			UpdateStockItemSummary(id, typeId, qty, StatusChange{from: "onHand", to: "onHold"}, txn)
		if err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (service *inventoryService) ReleaseItems(refNum string) error {
	txn := service.db.Begin()

	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"ref_num": sql.NullString{String: "", Valid: false},
		"status":  "onHand",
	}

	// extract summary update logic
	res := []*struct {
		StockItemID uint
		Qty         int
	}{}

	txn.Table("stock_item_units u").
		Select("u.stock_item_id, sum(1) as qty").
		Joins("left join stock_items si on si.id = u.stock_item_id").
		Where("u.ref_num = ?", refNum).
		Group("u.stock_item_id").
		Find(&res)

	result := txn.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Updates(updateWith)
	if result.Error != nil {
		txn.Rollback()
		return result.Error
	}

	if result.RowsAffected == 0 {
		txn.Rollback()
		return fmt.Errorf(`No stock item units associated with "%s"`, refNum)
	}

	typeId := models.StockItemTypes().Sellable
	for _, item := range res {
		err := service.summaryService.
			UpdateStockItemSummary(item.StockItemID, typeId, item.Qty, StatusChange{from: "onHold", to: "onHand"}, txn)
		if err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func onHandStockItemUnits(stockItemID uint, typeId uint, count int, db *gorm.DB) ([]uint, error) {
	var ids []uint
	err := db.Model(&models.StockItemUnit{}).
		Limit(count).
		Order("created_at").
		Where("stock_item_id = ?", stockItemID).
		Where("type_id = ?", typeId).
		Where("status = ?", "onHand").
		Pluck("id", &ids).
		Error

	println("onHandStockItemUnits", stockItemID, len(ids))

	if err != nil {
		return ids, err
	}

	if len(ids) < count {
		err := fmt.Errorf("Not enough onHand units for stock item %d of type %d. Expected %d, got %d",
			stockItemID, typeId, count, len(ids))

		return ids, err
	}

	return ids, nil
}
