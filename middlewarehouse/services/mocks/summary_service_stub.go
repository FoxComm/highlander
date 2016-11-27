package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type SummaryServiceStub struct {
}

func (repository *SummaryServiceStub) CreateStockItemSummary(stockItemId uint) exceptions.IException {
	return nil
}

func (repository *SummaryServiceStub) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) exceptions.IException {
	return nil
}

func (repository *SummaryServiceStub) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) exceptions.IException {
	return nil
}

func (repository *SummaryServiceStub) GetSummary() ([]*models.StockItemSummary, exceptions.IException) {
	return []*models.StockItemSummary{}, nil
}

func (repository *SummaryServiceStub) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, exceptions.IException) {
	return []*models.StockItemSummary{}, nil
}
