package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint, skuId uint, skuCode string) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SkuID:           skuId,
		SkuCode:         skuCode,
		DefaultUnitCost: 5000,
	}
}
