package repositories

import (
	"database/sql"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

const (
	ErrorNotEnoughStockItemUnits = "Oops! Looks like SKU %s is out of stock. Please remove it from the cart to complete checkout."
)

type stockItemUnitRepository struct {
	db *gorm.DB
}

type IStockItemUnitRepository interface {
	GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, exceptions.IException)
	GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, exceptions.IException)
	HoldUnitsInOrder(refNum string, ids []uint) (int, exceptions.IException)
	ReserveUnitsInOrder(refNum string) (int, exceptions.IException)
	UnsetUnitsInOrder(refNum string) (int, exceptions.IException)
	GetUnitForLineItem(refNum string, sku string) (*models.StockItemUnit, exceptions.IException)

	GetReleaseQtyByRefNum(refNum string) ([]*models.Release, exceptions.IException)

	CreateUnits(units []*models.StockItemUnit) exceptions.IException
	DeleteUnits(ids []uint) exceptions.IException
}

func NewStockItemUnitRepository(db *gorm.DB) IStockItemUnitRepository {
	return &stockItemUnitRepository{db}
}

func (repository *stockItemUnitRepository) CreateUnits(units []*models.StockItemUnit) exceptions.IException {
	txn := repository.db.Begin()
	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return NewDatabaseException(err)
		}
	}

	return NewDatabaseException(txn.Commit().Error)
}

func (repository *stockItemUnitRepository) DeleteUnits(ids []uint) exceptions.IException {
	return NewDatabaseException(repository.db.Delete(models.StockItemUnit{}, "id in (?)", ids).Error)
}

func (repository *stockItemUnitRepository) GetStockItemUnitIDs(stockItemID uint, unitStatus models.UnitStatus, unitType models.UnitType, count int) ([]uint, exceptions.IException) {
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
		return ids, NewDatabaseException(err)
	}

	if len(ids) < count {
		stockItem := &models.StockItem{}
		repository.db.First(stockItem, stockItemID)
		err := fmt.Errorf(ErrorNotEnoughStockItemUnits, stockItem.SKU)

		return ids, NewOutOfStockException(stockItem.SKU, count, len(ids), unitStatus, unitType, err)
	}

	return ids, nil
}

func (repository *stockItemUnitRepository) GetUnitsInOrder(refNum string) ([]*models.StockItemUnit, exceptions.IException) {
	var units []*models.StockItemUnit
	err := repository.db.
		Preload("StockItem").
		Where("stock_item_units.ref_num = ?", refNum).
		Find(&units).
		Error

	if err != nil {
		return nil, NewDatabaseException(err)
	}

	return units, nil
}

func (repository *stockItemUnitRepository) GetUnitForLineItem(refNum string, sku string) (*models.StockItemUnit, exceptions.IException) {
	unit := new(models.StockItemUnit)
	err := repository.db.
		Joins("JOIN stock_items ON stock_items.id = stock_item_units.stock_item_id").
		Where("stock_items.sku = ?", sku).
		Where("stock_item_units.ref_num = ?", refNum).
		Where("stock_item_units.status = ?", "onHold").
		First(unit).
		Error

	return unit, NewDatabaseException(err)
}

func (repository *stockItemUnitRepository) HoldUnitsInOrder(refNum string, ids []uint) (int, exceptions.IException) {
	updateWith := models.StockItemUnit{
		RefNum: sql.NullString{String: refNum, Valid: true},
		Status: models.StatusOnHold,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("id in (?)", ids).Updates(updateWith)

	return int(result.RowsAffected), NewDatabaseException(result.Error)
}

func (repository *stockItemUnitRepository) ReserveUnitsInOrder(refNum string) (int, exceptions.IException) {
	updateWith := models.StockItemUnit{
		Status: models.StatusReserved,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), NewDatabaseException(result.Error)
}

func (repository *stockItemUnitRepository) UnsetUnitsInOrder(refNum string) (int, exceptions.IException) {
	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"ref_num": sql.NullString{String: "", Valid: false},
		"status":  models.StatusOnHand,
	}

	result := repository.db.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), NewDatabaseException(result.Error)
}

func (repository *stockItemUnitRepository) GetReleaseQtyByRefNum(refNum string) ([]*models.Release, exceptions.IException) {
	res := []*models.Release{}

	err := repository.db.Table("stock_item_units u").
		Select("u.stock_item_id, sum(1) as qty").
		Joins("left join stock_items si on si.id = u.stock_item_id").
		Where("u.ref_num = ?", refNum).
		Group("u.stock_item_id").
		Find(&res).
		Error

	return res, NewDatabaseException(err)
}

type outOfStockException struct {
	cls        string `json:"type"`
	sku        string
	wanted     int
	have       int
	unitStatus models.UnitStatus
	unitType   models.UnitType
	exceptions.Exception
}

func NewOutOfStockException(sku string, wanted int, have int, unitStatus models.UnitStatus, unitType models.UnitType, error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return outOfStockException{
		cls:        "outOfStock",
		sku:        sku,
		wanted:     wanted,
		have:       have,
		unitStatus: unitStatus,
		unitType:   unitType,
		Exception:  exceptions.Exception{error},
	}
}
