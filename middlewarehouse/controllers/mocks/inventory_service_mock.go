package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type InventoryServiceMock struct {
	mock.Mock
}

func (m *InventoryServiceMock) GetStockItems() ([]*models.StockItem, exceptions.IException) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *InventoryServiceMock) GetStockItemById(id uint) (*models.StockItem, exceptions.IException) {
	args := m.Called(id)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *InventoryServiceMock) GetAFSByID(id uint, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	args := m.Called(id, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}
	return nil, nil
}

func (m *InventoryServiceMock) GetAFSBySKU(sku string, unitType models.UnitType) (*models.AFS, exceptions.IException) {
	args := m.Called(sku, unitType)

	if model, ok := args.Get(0).(*models.AFS); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *InventoryServiceMock) CreateStockItem(stockItem *models.StockItem) (*models.StockItem, exceptions.IException) {
	args := m.Called(stockItem)

	if model, ok := args.Get(0).(*models.StockItem); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *InventoryServiceMock) IncrementStockItemUnits(id uint, unitType models.UnitType, units []*models.StockItemUnit) exceptions.IException {
	args := m.Called(id, unitType, units)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *InventoryServiceMock) DecrementStockItemUnits(id uint, unitType models.UnitType, qty int) exceptions.IException {
	args := m.Called(id, unitType, qty)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *InventoryServiceMock) HoldItems(refNum string, skus map[string]int) exceptions.IException {
	args := m.Called(refNum, skus)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *InventoryServiceMock) ReserveItems(refNum string) exceptions.IException {
	args := m.Called(refNum)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *InventoryServiceMock) ReleaseItems(refNum string) exceptions.IException {
	args := m.Called(refNum)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
