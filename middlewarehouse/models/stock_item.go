package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockItem struct {
	gormfox.Base
	SKU             string
	StockLocation   StockLocation
	StockLocationID uint
	DefaultUnitCost int
	Scope           string
}

func (si StockItem) Identifier() uint {
	return si.ID
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	return &StockItem{
		StockLocationID: payload.StockLocationID,
		SKU:             payload.SKU,
		DefaultUnitCost: payload.DefaultUnitCost,
		Scope:           payload.Scope,
	}
}
