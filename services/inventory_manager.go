package services

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/logging"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type InventoryMgr struct {
	ctx    *store.StoreContext
	db     *gorm.DB
	logger logging.Logger
}

// NewInventoryMgr creates a new InventoryMgr.
func NewInventoryMgr(c *store.StoreContext) (*InventoryMgr, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	im := &InventoryMgr{
		ctx:    c,
		logger: logging.Log,
		db:     db,
	}

	return im, nil
}

func (im *InventoryMgr) FindStockItemByID(id uint) (*responses.StockItem, error) {
	si := &models.StockItem{}
	if err := im.db.First(si, id).Error; err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(si), nil
	}
}

func (im *InventoryMgr) CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	si := models.NewStockItemFromPayload(payload)

	if err := im.db.Create(si).Error; err != nil {
		return nil, err
	}

	return responses.NewStockItemFromModel(si), nil
}

func (im *InventoryMgr) IncrementStockItemUnits(payload *payloads.StockItemUnits) error {
	units := models.NewStockItemUnitsFromPayload(payload)

	txn := im.db.Begin()

	for _, v := range units {
		if err := txn.Create(v).Error; err != nil {
			txn.Rollback()
			return err
		}
	}

	return txn.Commit().Error
}

func (im *InventoryMgr) DecrementStockItemUnits(payload *payloads.StockItemUnits) error {
	return nil
}
