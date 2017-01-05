package repositories

import (
	"database/sql"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/transaction"
	"github.com/jinzhu/gorm"
)

const (
	ErrorNotEnoughStockItemUnits = "Not enough units of status %s for stock item %d of type %v."
)

type stockItemUnitRepository struct {
	db *gorm.DB
}

type IStockItemUnitRepository interface {
	WithTransaction(txn *gorm.DB) IStockItemUnitRepository
	GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, error)
	GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, error)
	GetUnitForLineItem(refNum string, sku string) (*models.StockItemUnit, error)
	GetQtyForOrder(refNum string) ([]*models.Release, error)

	HoldUnitsInOrder(refNum string, ids []uint) (int, error)
	ReserveUnitsInOrder(refNum string) (int, error)
	UnsetUnitsInOrder(refNum string) (int, error)
	ShipUnitsInOrder(refNum string) (int, error)
	DeleteUnitsInOrder(refNum string) (int, error)

	CreateUnits(units []*models.StockItemUnit) error
	DeleteUnits(ids []uint) error

	HoldUnitForLineItem(skuID uint, orderRefNum, lineItemRefNum string) (*models.StockItemUnit, error)
}

func NewStockItemUnitRepository(db *gorm.DB) IStockItemUnitRepository {
	return &stockItemUnitRepository{db}
}

// WithTransaction returns a shallow copy of repository with its db changed to txn. The provided txn must be non-nil.
func (repository *stockItemUnitRepository) WithTransaction(txn *gorm.DB) IStockItemUnitRepository {
	if txn == nil {
		panic("nil transaction")
	}

	return NewStockItemUnitRepository(txn)
}

func (repository *stockItemUnitRepository) CreateUnits(units []*models.StockItemUnit) error {
	txn := transaction.NewTransaction(repository.db).Begin()
	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (repository *stockItemUnitRepository) DeleteUnits(ids []uint) error {
	return repository.db.Delete(models.StockItemUnit{}, "id in (?)", ids).Error
}

func (repository *stockItemUnitRepository) GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, error) {
	var ids []uint
	err := repository.db.Model(&models.StockItemUnit{}).
		Limit(count).
		Order("created_at").
		Where("stock_item_id = ?", stockItemID).
		Where("type = ?", unitType).
		Where("status = ?", string(unitStatus)).
		Pluck("id", &ids).
		Error

	if err != nil {
		return ids, err
	}

	if len(ids) < count {
		err := fmt.Errorf(ErrorNotEnoughStockItemUnits, unitStatus, stockItemID, unitType)

		return ids, err
	}

	return ids, nil
}

func (repository *stockItemUnitRepository) GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, error) {
	var units []*models.StockItemUnit
	err := repository.db.
		Preload("StockItem").
		Where("stock_item_units.order_ref_num = ?", refNum).
		Find(&units).
		Error

	if err != nil {
		return nil, err
	}

	return units, nil
}

func (repository *stockItemUnitRepository) GetUnitForLineItem(refNum string, sku string) (*models.StockItemUnit, error) {
	unit := new(models.StockItemUnit)
	err := repository.db.
		Joins("JOIN stock_items ON stock_items.id = stock_item_units.stock_item_id").
		Where("stock_items.sku = ?", sku).
		Where("stock_item_units.order_ref_num = ?", refNum).
		Where("stock_item_units.status = ?", "onHold").
		First(unit).
		Error

	return unit, err
}

func (repository *stockItemUnitRepository) HoldUnitForLineItem(skuID uint, orderRefNum, lineItemRefNum string) (*models.StockItemUnit, error) {
	unit := new(models.StockItemUnit)
	err := repository.db.
		Set("gorm:query_option", "FOR UPDATE").
		Joins("JOIN stock_items ON stock_items.id = stock_item_units.stock_item_id").
		Where("stock_items.sku_id = ?", skuID).
		Where("stock_item_units.status = ?", "onHand").
		First(unit).
		Error

	if err != nil {
		return nil, err
	}

	updateMap := map[string]interface{}{
		"order_ref_num":     orderRefNum,
		"line_item_ref_num": lineItemRefNum,
	}

	err = repository.db.Model(unit).Updates(updateMap).Error
	return unit, err
}

func (repository *stockItemUnitRepository) HoldUnitsInOrder(refNum string, ids []uint) (int, error) {
	updateWith := models.StockItemUnit{
		OrderRefNum: sql.NullString{String: refNum, Valid: true},
		Status:      models.StatusOnHold,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("id in (?)", ids).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) ReserveUnitsInOrder(refNum string) (int, error) {
	updateWith := models.StockItemUnit{
		Status: models.StatusReserved,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("order_ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) ShipUnitsInOrder(refNum string) (int, error) {
	updateWith := map[string]interface{}{
		"status": models.StatusShipped,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("order_ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) UnsetUnitsInOrder(refNum string) (int, error) {
	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"order_ref_num": sql.NullString{String: "", Valid: false},
		"status":        models.StatusOnHand,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("order_ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) DeleteUnitsInOrder(refNum string) (int, error) {
	result := repository.db.Delete(models.StockItemUnit{}, "order_ref_num = ?", refNum)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) GetQtyForOrder(refNum string) ([]*models.Release, error) {
	res := []*models.Release{}

	err := repository.db.Table("stock_item_units u").
		Select("u.stock_item_id, sum(1) as qty").
		Joins("left join stock_items si on si.id = u.stock_item_id").
		Where("u.order_ref_num = ?", refNum).
		Group("u.stock_item_id").
		Find(&res).
		Error

	return res, err
}
