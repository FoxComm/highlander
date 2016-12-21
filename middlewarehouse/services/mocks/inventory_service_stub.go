package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type InventoryServiceStub struct {
}

func (repository *InventoryServiceStub) GetStockItems() ([]*models.StockItem, error) {
	return nil, nil
}

func (repository *InventoryServiceStub) GetStockItemById(id uint) (*models.StockItem, error) {
	return nil, nil
}

func (repository *InventoryServiceStub) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	return nil, nil
}

func (repository *InventoryServiceStub) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	return nil, nil
}

func (repository *InventoryServiceStub) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	return nil, nil
}

func (repository *InventoryServiceStub) IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	return nil
}

func (repository *InventoryServiceStub) DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error {
	return nil
}

func (repository *InventoryServiceStub) HoldItems(refNum string, skus map[string]int) error {
	return nil
}

func (repository *InventoryServiceStub) ReserveItems(refNum string) error {
	return nil
}

func (repository *InventoryServiceStub) ReleaseItems(refNum string) error {
	return nil
}

func (repository *InventoryServiceStub) DeleteItems(refNum string) error {
	return nil
}
