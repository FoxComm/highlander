package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockItem struct {
	ID              uint   `json:"id"`
	SkuID           uint   `json:"skuId"`
	SkuCode         string `json:"skuCode"`
	StockLocationID uint   `json:"stockLocationId"`
	DefaultUnitCost int    `json:"defaultUnitCost"`
}

func NewStockItemFromModel(si *models.StockItem) *StockItem {
	return &StockItem{
		ID:              si.ID,
		SkuID:           si.SkuID,
		SkuCode:         si.SkuCode,
		StockLocationID: si.StockLocationID,
		DefaultUnitCost: si.DefaultUnitCost,
	}
}
