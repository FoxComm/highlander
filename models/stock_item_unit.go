package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockItemUnit struct {
	gormfox.Base
	StockItemID uint
	UnitCost    int
	Status      string
}

func (siu StockItemUnit) Identifier() uint {
	return siu.ID
}

func NewStockItemUnitsFromPayload(payload *payloads.StockItemUnits) []*StockItemUnit {
	units := []*StockItemUnit{}

	for i := 0; i < payload.Qty; i++ {
		item := &StockItemUnit{
			StockItemID: payload.StockItemID,
			UnitCost:    payload.UnitCost,
			Status:      payload.Status,
		}
		units = append(units, item)
	}

	return units
}
