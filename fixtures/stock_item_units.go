package fixtures

import (
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"
)

func GetStockItemUnit(id uint, stockItem *models.StockItem) *models.StockItemUnit {
	return &models.StockItemUnit{
		Base: gormfox.Base{
			ID: id,
		},
		StockItemID: stockItem.ID,
		StockItem:   *stockItem,
		UnitCost:    500,
		Type:        models.Sellable,
		Status:      models.StatusOnHand,
	}
}

func GetStockItemUnits(stockItem *models.StockItem, qty int) []*models.StockItemUnit {
	units := []*models.StockItemUnit{}
	for i := 0; i < qty; i++ {
		item := GetStockItemUnit(uint(i), stockItem)
		units = append(units, item)
	}

	return units
}
