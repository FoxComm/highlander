package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	StockLocationID uint
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	item := &StockItem{StockLocationID: payload.StockLocationID}
	return item
}

func MakeStockItemFromPayload(payload payloads.StockItem) StockItem {
	item := StockItem{StockLocationID: payload.StockLocationID}
	return item
}
