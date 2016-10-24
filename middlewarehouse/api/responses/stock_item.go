package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockItem struct {
	ID              uint   `json:"id"`
	SKU             string `json:"sku"`
	StockLocationID uint   `json:"stockLocationId"`
	DefaultUnitCost int    `json:"defaultUnitCost"`
	Scope           string `json:"scope"`
}

func NewStockItemFromModel(si *models.StockItem) *StockItem {
	return &StockItem{
		ID:              si.ID,
		SKU:             si.SKU,
		StockLocationID: si.StockLocationID,
		DefaultUnitCost: si.DefaultUnitCost,
		Scope:           si.Scope,
	}
}
