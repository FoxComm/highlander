package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type StockItem struct {
	SkuID           uint `json:"skuId" binding:"required"`
	StockLocationID uint `json:"stockLocationId" binding:"required"`
	DefaultUnitCost int  `json:"defaultUnitCost"`
}

func (payload *StockItem) Model() *models.StockItem {
	return &models.StockItem{
		StockLocationID: payload.StockLocationID,
		SkuID:           payload.SkuID,
		DefaultUnitCost: payload.DefaultUnitCost,
	}
}
