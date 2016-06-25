package services

import (
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

func (im *InventoryMgr) FindStockItemID(id uint) (*responses.StockItem, error) {
	si := &models.StockItem{}
	if item, err := im.repo.FindByID(si, id); err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(item.(*models.StockItem)), nil
	}
}
