package fixtures

import (
	"github.com/FoxComm/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SKU:             "TEST-DEFAULT",
		DefaultUnitCost: 5000,
	}
}
