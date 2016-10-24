package models

import (
	"database/sql"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockItemUnit struct {
	gormfox.Base
	StockItemID uint
	StockItem   StockItem
	Type        UnitType
	RefNum      sql.NullString
	UnitCost    int
	Status      UnitStatus
	Scope       string
}

func (siu StockItemUnit) Identifier() uint {
	return siu.ID
}

func NewStockItemUnitsFromPayload(stockItemID uint, payload *payloads.IncrementStockItemUnits) []*StockItemUnit {
	units := []*StockItemUnit{}

	for i := 0; i < payload.Qty; i++ {
		item := &StockItemUnit{
			StockItemID: stockItemID,
			UnitCost:    payload.UnitCost,
			Status:      UnitStatus(payload.Status),
			Type:        UnitType(payload.Type),
			Scope:       payload.Scope,
		}
		units = append(units, item)
	}

	return units
}
