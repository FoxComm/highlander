package services

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/jinzhu/gorm"
)

type SummaryServiceStub struct {
}

func (service *SummaryServiceStub) WithTransaction(txn *gorm.DB) ISummaryService {
	return service
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

func (repository *SummaryServiceStub) GetSummaryBySKU(sku string) ([]*models.StockItemSummary, error) {
	return []*models.StockItemSummary{}, nil
}
