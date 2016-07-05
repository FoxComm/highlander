package services

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/common/logging"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/models"
)

type InventoryMgr struct {
	ctx    *store.StoreContext
	repo   gormfox.Repository
	logger logging.Logger
}

// NewInventoryMgr creates a new InventoryMgr.
func NewInventoryMgr(c *store.StoreContext) (*InventoryMgr, error) {
	repo, err := gormfox.NewRepository()
	if err != nil {
		return nil, err
	}

	im := &InventoryMgr{
		ctx:    c,
		logger: logging.Log,
		repo:   repo,
	}

	return im, nil
}

func (im *InventoryMgr) FindStockItemByID(id uint) (*responses.StockItem, error) {
	si := &models.StockItem{}
	if item, err := im.repo.FindByID(si, id); err != nil {
		fmt.Printf("Err: %s\n", err)
		return nil, err
	} else {
		return responses.NewStockItemFromModel(item.(*models.StockItem)), nil
	}
}

func (im *InventoryMgr) CreateStockItem(payload *payloads.StockItem) (*responses.StockItem, error) {
	si := models.NewStockItemFromPayload(payload)

	if err := im.repo.Create(si); err != nil {
		return nil, err
	}

	return responses.NewStockItemFromModel(si), nil
}

func (im *InventoryMgr) IncrementStockItemUnits(payload *payloads.StockItemUnits) error {
	units := models.NewStockItemUnitsFromPayload(payload)

	err := im.repo.BeginTxn()
	if err != nil {
		return err
	}

	for _, v := range units {
		if err := im.repo.Create(v); err != nil {
			return err
		}
	}

	return im.repo.CommitTxn()
}

func (im *InventoryMgr) DecrementStockItemUnits(payload *payloads.StockItemUnits) error {
	return nil
}
