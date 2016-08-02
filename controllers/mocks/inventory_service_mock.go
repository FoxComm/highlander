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

func (m *InventoryServiceMock) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	args := m.Called(stockItem)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *InventoryServiceMock) IncrementStockItemUnits(id, locationId, typeId uint, units []*models.StockItemUnit) error {
	args := m.Called(id, locationId, typeId, units)

	return args.Error(0)
}

func (m *InventoryServiceMock) DecrementStockItemUnits(id, locationId, typeId uint, qty int) error {
	args := m.Called(id, locationId, typeId, qty)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReserveItems(refNum string, locationId uint, skus map[string]int) error {
	args := m.Called(refNum, locationId, skus)

	return args.Error(0)
}

func (m *InventoryServiceMock) ReleaseItems(refNum string, locationId uint) error {
	args := m.Called(refNum, locationId)

	return args.Error(0)
}
