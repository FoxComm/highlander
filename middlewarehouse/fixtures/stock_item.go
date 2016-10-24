package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint, sku string) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SkuID:           1,
		SkuCode:         sku,
		DefaultUnitCost: 5000,
	}
}
