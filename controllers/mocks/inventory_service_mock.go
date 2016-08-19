package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/stretchr/testify/mock"
)

type InventoryServiceMock struct {
	mock.Mock
}

func (m *InventoryServiceMock) GetStockItems() ([]*models.StockItem, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetStockItemById(id uint) (*models.StockItem, error) {
	args := m.Called(id)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, error) {
	args := m.Called(id, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, error) {
	args := m.Called(sku, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	args := m.Called(stockItem)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) error {
	args := m.Called(id, unitType, units)

	return args.Error(0)
}

func (m *InventoryServiceMock) DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) error {
	args := m.Called(id, unitType, qty)

	return args.Error(0)
}

func (m *InventoryServiceMock) HoldItems(refNum string, skus map[string]int) error {
	args := m.Called(refNum, skus)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReserveItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReleaseItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}
