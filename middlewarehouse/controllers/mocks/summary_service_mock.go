package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
)

type SummaryServiceMock struct {
	mock.Mock
}

func (m *SummaryServiceMock) WithTransaction(txn *gorm.DB) services.SummaryService {
	return m
}

// implement service interface to pass mock as service (another solution?)
func (m *SummaryServiceMock) GetSummary() ([]*models.StockItemSummary, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *SummaryServiceMock) GetSummaryBySKU(code string) ([]*models.StockItemSummary, error) {
	args := m.Called(code)

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m *SummaryServiceMock) CreateStockItemSummary(stockItemId uint) error {
	args := m.Called(stockItemId)

	return args.Error(0)
}

func (m *SummaryServiceMock) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error {
	args := m.Called(stockItemId, unitType, qty, status)

	return args.Error(0)
}

func (m *SummaryServiceMock) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error {
	args := m.Called(summary, status, qty)

	return args.Error(0)
}
