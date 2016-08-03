package repositories

import (
	"database/sql"
	"fmt"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/jinzhu/gorm"
)

type stockItemUnitRepository struct {
	db *gorm.DB
}

type IStockItemUnitRepository interface {
	OnHandStockItemUnits(stockItemID uint, typeId uint, count int) ([]uint, error)
	SetUnitsInOrder(refNum string, ids []uint, dbContext *gorm.DB) (int, error)
	UnsetUnitsInOrder(refNum string, dbContext *gorm.DB) (int, error)

	GetReleaseQtyByRefNum(refNum string) ([]*models.Release, error)

	CreateUnits(units []*models.StockItemUnit, dbContext *gorm.DB) error
	DeleteUnits(ids []uint, dbContext *gorm.DB) error
}

func NewStockItemUnitRepository(db *gorm.DB) IStockItemUnitRepository {
	return &stockItemUnitRepository{db}
}

func (repository *stockItemUnitRepository) resolveDb(db *gorm.DB) *gorm.DB {
	if db != nil {
		return db
	} else {
		return repository.db
	}
}

func (repository *stockItemUnitRepository) CreateUnits(units []*models.StockItemUnit, dbContext *gorm.DB) error {
	db := repository.resolveDb(dbContext)

	for _, v := range units {
		if err := db.Create(v).Error; err != nil {
			return err
		}
	}

	return nil
}

func (repository *stockItemUnitRepository) DeleteUnits(ids []uint, dbContext *gorm.DB) error {
	db := repository.resolveDb(dbContext)

	return db.Delete(models.StockItemUnit{}, "id in (?)", ids).Error
}

func (repository *stockItemUnitRepository) OnHandStockItemUnits(stockItemID uint, typeId uint, count int) ([]uint, error) {
	var ids []uint
	err := repository.db.Model(&models.StockItemUnit{}).
		Limit(count).
		Order("created_at").
		Where("stock_item_id = ?", stockItemID).
		Where("type_id = ?", typeId).
		Where("status = ?", "onHand").
		Pluck("id", &ids).
		Error

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

func (repository *stockItemUnitRepository) SetUnitsInOrder(refNum string, ids []uint, dbContext *gorm.DB) (int, error) {
	db := repository.resolveDb(dbContext)

	updateWith := models.StockItemUnit{
		RefNum: sql.NullString{String: refNum, Valid: true},
		Status: "onHold",
	}

	result := db.Model(models.StockItemUnit{}).Where("id in (?)", ids).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) UnsetUnitsInOrder(refNum string, dbContext *gorm.DB) (int, error) {
	db := repository.resolveDb(dbContext)

	// gorm does not update empty fields when updating with struct, so use map here
	updateWith := map[string]interface{}{
		"ref_num": sql.NullString{String: "", Valid: false},
		"status":  "onHand",
	}

	result := db.Model(&models.StockItemUnit{}).Where("ref_num = ?", refNum).Updates(updateWith)

	return int(result.RowsAffected), result.Error
}

func (repository *stockItemUnitRepository) GetReleaseQtyByRefNum(refNum string) ([]*models.Release, error) {
	res := []*models.Release{}

	err := repository.db.Table("stock_item_units u").
		Select("u.stock_item_id, sum(1) as qty").
		Joins("left join stock_items si on si.id = u.stock_item_id").
		Where("u.ref_num = ?", refNum).
		Group("u.stock_item_id").
		Find(&res).
		Error

	return res, err
}
