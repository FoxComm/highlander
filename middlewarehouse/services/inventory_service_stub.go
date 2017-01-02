package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type InventoryServiceStub struct {
}

func (service *InventoryServiceStub) WithTransaction(txn *gorm.DB) IInventoryService {
	return service
}

func (service *InventoryServiceStub) GetStockItems() ([]*models.StockItem, error) {
	return nil, nil
}

func (service *InventoryServiceStub) GetStockItemById(id uint) (*models.StockItem, error) {
	return nil, nil
}

func (service *InventoryServiceStub) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	return nil, nil
}

func (service *InventoryServiceStub) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	return nil, nil
}

func (service *InventoryServiceStub) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	return nil, nil
}

func (service *InventoryServiceStub) IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	return nil
}

func (service *InventoryServiceStub) DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error {
	return nil
}

func (service *InventoryServiceStub) HoldItems(refNum string, skus map[string]int) error {
	return nil
}

func (service *InventoryServiceStub) ReserveItems(refNum string) error {
	return nil
}

func (service *InventoryServiceStub) ReleaseItems(refNum string) error {
	return nil
}

func (service *InventoryServiceStub) ShipItems(refNum string) error {
	return nil
}

func (service *InventoryServiceStub) DeleteItems(refNum string) error {
	return nil
}
