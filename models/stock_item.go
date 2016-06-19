package models

import (
	"github.com/FoxComm/middlewarehouse/api"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	StockLocationID uint
}

func NewStockItemFromAPI(si *api.StockItem) *StockItem {
	item := &StockItem{}
	item.StockLocationID = si.StockLocationID
	return item
}

func MakeStockItemFromAPI(si api.StockItem) StockItem {
	item := StockItem{}
	item.StockLocationID = si.StockLocationID
	return item
}
