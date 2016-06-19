package responses

import "github.com/FoxComm/middlewarehouse/models"

type StockItem struct {
	ID              uint `json:"id"`
	StockLocationID uint `json:"stockLocationId"`
}

func NewStockItemFromModel(si *models.StockItem) *StockItem {
	return &StockItem{ID: si.ID, StockLocationID: si.StockLocationID}
}
