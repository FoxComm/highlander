package responses

import "github.com/FoxComm/middlewarehouse/models"

type StockItem struct {
	ID              uint   `json:"id"`
	SKU             string `json:"sku"`
	StockLocationID uint   `json:"stockLocationId"`
}

func NewStockItemFromModel(si *models.StockItem) *StockItem {
	return &StockItem{
		ID:              si.ID,
		SKU:             si.SKU,
		StockLocationID: si.StockLocationID,
	}
}
