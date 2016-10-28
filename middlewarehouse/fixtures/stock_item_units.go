package fixtures

import "github.com/FoxComm/highlander/middlewarehouse/models"

func GetStockItemUnit(stockItem *models.StockItem) *models.StockItemUnit {
	return &models.StockItemUnit{
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
		item := GetStockItemUnit(stockItem)
		units = append(units, item)
	}

	return units
}
