package services

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/logging"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/repositories"
)

type InventoryManager struct {
	ctx                 *store.StoreContext
	stockItemRepository repositories.StockItemRepository
	logger              logging.Logger
}

// NewInventory creates an Inventory
func NewInventoryManager() *InventoryManager {
	return &InventoryManager{}
}

// Setup takes the StoreContext and configures the API
func (i *InventoryManager) setup(ctx *store.StoreContext) {
	i.logger = logging.Log
	itemRepo, err := repositories.NewStockItemRepository(ctx)
	if err != nil {
		i.logger.Errorf("Failure connecting to store database!", logging.E(err))
	}

	i.stockItemRepository = itemRepo
	i.ctx = ctx
}

func (i *InventoryManager) FindStockItem(ctx *store.StoreContext, id uint) (*responses.StockItem, error) {
	i.setup(ctx)
	if model, err := i.stockItemRepository.Find(id); err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(model), nil
	}
}

func (i *InventoryManager) CreateStockItem(ctx *store.StoreContext, item *payloads.StockItem) (*responses.StockItem, error) {
	i.setup(ctx)
	model := models.NewStockItemFromPayload(item)
	if err := i.stockItemRepository.Create(model); err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(model), nil
	}
}

func (i *InventoryManager) UpdateStockItem(ctx *store.StoreContext, item *payloads.StockItem) (*responses.StockItem, error) {
	i.setup(ctx)
	model := models.NewStockItemFromPayload(item)
	if err := i.stockItemRepository.Update(model); err != nil {
		return nil, err
	} else {
		return responses.NewStockItemFromModel(model), nil
	}
}
