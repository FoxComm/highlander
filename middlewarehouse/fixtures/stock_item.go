package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint, sku string) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SKU:             sku,
		DefaultUnitCost: 5000,
	}
}
