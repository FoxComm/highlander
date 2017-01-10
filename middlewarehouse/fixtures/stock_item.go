package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockItem(stockLocationId uint, skuID uint) *models.StockItem {
	return &models.StockItem{
		StockLocationID: stockLocationId,
		SkuID:           skuID,
		DefaultUnitCost: 5000,
	}
}
