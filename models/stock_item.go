package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	SKU             string
	StockLocation   StockLocation
	StockLocationID uint
	DefaultUnitCost int
}

func (si StockItem) Identifier() uint {
	return si.ID
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	item := &StockItem{
		StockLocationID: payload.StockLocationID,
		SKU:             payload.SKU,
		DefaultUnitCost: payload.DefaultUnitCost,
	}

	return item
}
