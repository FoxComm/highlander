package mocks

import (
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/stretchr/testify/mock"
)

type InventoryServiceMock struct {
	mock.Mock
}

func (m InventoryServiceMock) GetStockItems() ([]*models.StockItem, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m InventoryServiceMock) GetStockItemByID(id uint) (*models.StockItem, error) {
	args := m.Called(id)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m InventoryServiceMock) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, error) {
	args := m.Called(stockItem)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m InventoryServiceMock) IncrementStockItemUnits(id uint, units []*models.StockItemUnit) error {
	return nil
}

func (m InventoryServiceMock) DecrementStockItemUnits(id uint, qty int) error {
	return nil
}

func (m InventoryServiceMock) ReserveItems(refNum string, skus map[string]int) error {
	args := m.Called(refNum, skus)

	return args.Error(0)
}

func (m InventoryServiceMock) ReleaseItems(refNum string) error {
	args := m.Called(refNum)

	return args.Error(0)
}
