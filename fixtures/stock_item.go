package fixtures

import (
	"github.com/FoxComm/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint, sku string) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SKU:             sku,
		DefaultUnitCost: 5000,
	}
}
