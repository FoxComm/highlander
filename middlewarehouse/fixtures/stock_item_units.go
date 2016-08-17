package fixtures

import (
	"github.com/FoxComm/middlewarehouse/models"
)

func GetStockItemUnit(stockItemId uint) *models.StockItemUnit {
	return &models.StockItemUnit{
		StockItemID: stockItemId,
		UnitCost:    500,
		Type:        models.Sellable,
		Status:      models.StatusOnHand,
	}
}

func GetStockItemUnits(stockItemId uint, qty int) []*models.StockItemUnit {
	units := []*models.StockItemUnit{}
	for i := 0; i < qty; i++ {
		item := GetStockItemUnit(stockItemId)
		units = append(units, item)
	}

	return units
}
