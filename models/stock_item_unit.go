package models

import (
	"database/sql"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockItemUnit struct {
	gormfox.Base
	StockItemID   uint
	ReservationID sql.NullInt64
	UnitCost      int
	Status        string
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
			Status:      payload.Status,
		}
		units = append(units, item)
	}

	return units
}
