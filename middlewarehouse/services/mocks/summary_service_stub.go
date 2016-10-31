package mocks

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type SummaryServiceStub struct {
}

func (repository *SummaryServiceStub) CreateStockItemSummary(stockItemId uint) error {
	return nil
}

func (repository *SummaryServiceStub) UpdateStockItemSummary(stockItemId uint, unitType models.UnitType, qty int, status models.StatusChange) error {
	return nil
}

func (repository *SummaryServiceStub) CreateStockItemTransaction(summary *models.StockItemSummary, status models.UnitStatus, qty int) error {
	return nil
}

func (repository *SummaryServiceStub) GetSummary() ([]*models.StockItemSummary, error) {
	return []*models.StockItemSummary{}, nil
}

func (repository *SummaryServiceStub) GetSummaryBySKU(skuId uint) ([]*models.StockItemSummary, error) {
	return []*models.StockItemSummary{}, nil
}
