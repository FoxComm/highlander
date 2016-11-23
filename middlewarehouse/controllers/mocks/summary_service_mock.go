package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/mock"
)

type SummaryServiceMock struct {
	mock.Mock
}

// implement service interface to pass mock as service (another solution?)
func (m *SummaryServiceMock) GetSummary() ([]*models.StockItemSummary, exceptions.IException) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *SummaryServiceMock) GetSummaryBySKU(code string) ([]*models.StockItemSummary, exceptions.IException) {
	args := m.Called(code)

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	if ex, ok := args.Get(1).(exceptions.IException); ok {
		return nil, ex
	}

	return nil, nil
}

func (m *SummaryServiceMock) CreateStockItemSummary(stockItemId uint) exceptions.IException {
	args := m.Called(stockItemId)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *SummaryServiceMock) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) exceptions.IException {
	args := m.Called(stockItemId, unitType, qty, status)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}

func (m *SummaryServiceMock) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) exceptions.IException {
	args := m.Called(summary, status, qty)

	if ex, ok := args.Get(0).(exceptions.IException); ok {
		return ex
	}

	return nil
}
