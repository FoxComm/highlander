package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

func GetStockItemSummary(stockItem *models.StockItem, unitType models.UnitType, afs int) *models.StockItemSummary {
	return &models.StockItemSummary{
		StockItem:   *stockItem,
		StockItemID: stockItem.ID,
		Type:        unitType,
		AFS:         afs,
	}
}
