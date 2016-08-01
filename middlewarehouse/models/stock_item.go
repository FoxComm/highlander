package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	SKU             string
	StockLocationID uint
}

func (si StockItem) Identifier() uint {
	return si.ID
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	item := &StockItem{StockLocationID: payload.StockLocationID, SKU: payload.SKU}
	return item
}
