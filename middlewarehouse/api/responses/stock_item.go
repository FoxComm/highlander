package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockItem struct {
	ID              uint   `json:"id"`
	SKU             string `json:"sku"`
	StockLocationID uint   `json:"stockLocationId"`
	DefaultUnitCost int    `json:"defaultUnitCost"`
}

func NewStockItemFromModel(si *models.StockItem) *StockItem {
	return &StockItem{
		ID:              si.ID,
		SKU:             si.SKU.Code,
		StockLocationID: si.StockLocationID,
		DefaultUnitCost: si.DefaultUnitCost,
	}
}
