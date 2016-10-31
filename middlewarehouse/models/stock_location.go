package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockLocation struct {
	gormfox.Base
	Name    string
	Type    string
	Address *Address
	Scope   string
}

func NewStockLocationFromPayload(payload *payloads.StockLocation) *StockLocation {
	var address *Address
	if payload.Address != nil {
		address = NewAddressFromPayload(payload.Address)
	}

	location := &StockLocation{
		Name:    payload.Name,
		Type:    payload.Type,
		Address: address,
		Scope:   payload.Scope,
	}

	return location
}
