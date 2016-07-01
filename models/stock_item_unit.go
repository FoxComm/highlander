package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/common/validation"
)

type StockItemUnit struct {
	gormfox.Base
	StockItemID uint
	UnitCost    uint
	Status      string
}

func (siu StockItemUnit) Identifier() uint {
	return siu.ID
}

func (siu StockItemUnit) Validate(repository gormfox.Repository) ([]validation.Invalid, error) {
	return []validation.Invalid{}, nil
}

func NewStockItemUnitsFromPayload(payload *payloads.IncrementStockItemUnits) []*StockItemUnit {
	units := []*StockItemUnit{}

	for i := uint(0); i < payload.Qty; i++ {
		item := &StockItemUnit{
			StockItemID: payload.StockItemID,
			UnitCost:    payload.UnitCost,
			Status:      payload.Status,
		}
		units = append(units, item)
	}

	return units
}
