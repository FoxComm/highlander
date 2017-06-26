package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockItem struct {
	SKU             string `json:"sku" binding:"required"`
	StockLocationID uint   `json:"stockLocationId" binding:"required"`
	DefaultUnitCost int    `json:"defaultUnitCost"`
}

func (payload *StockItem) Model() *models.StockItem {
	return &models.StockItem{
		StockLocationID: payload.StockLocationID,
		SKU:             payload.SKU,
		DefaultUnitCost: payload.DefaultUnitCost,
	}
}
