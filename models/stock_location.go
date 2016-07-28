package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type StockLocation struct {
	gormfox.Base
	Name    string
	Type    string
	Address *Address
}

func NewStockLocationFromPayload(payload *payloads.StockLocation) *StockLocation {
	location := &StockLocation{
		Name:    payload.Name,
		Type:    payload.Type,
		Address: NewAddressFromPayload(payload.Address),
	}

	return location
}
