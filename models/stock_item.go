package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/common/validation"
)

type StockItem struct {
	gormfox.Base
	StockLocationID uint
}

func (si StockItem) Identifier() uint {
	return si.ID
}

func (si StockItem) Validate(repository gormfox.Repository) ([]validation.Invalid, error) {
	return []validation.Invalid{}, nil
}

func NewStockItemFromPayload(payload *payloads.StockItem) *StockItem {
	item := &StockItem{StockLocationID: payload.StockLocationID}
	return item
}
