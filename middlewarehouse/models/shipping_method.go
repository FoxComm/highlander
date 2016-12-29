package models

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

const (
	ShippingTypeFlat = iota
	ShippingTypeVariable
)

type ShippingMethod struct {
	gormfox.Base
	CarrierID    uint
	Carrier      Carrier
	Name         string
	Code         string
	ShippingType int
	Cost         uint
	Scope        string

	Conditions   QueryStatement
	Restrictions QueryStatement
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}

func NewShippingMethodFromPayload(payload *payloads.ShippingMethod) (*ShippingMethod, error) {
	sm := &ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
		Code:      payload.Code,
		Cost:      payload.Cost,
		Scope:     payload.Scope,
	}

	switch payload.ShippingType {
	case "flat":
		sm.ShippingType = ShippingTypeFlat
	case "variable":
		sm.ShippingType = ShippingTypeVariable
	default:
		return nil, fmt.Errorf("Expected shipping type flat or variable, got: %s", payload.ShippingType)
	}

	return sm, nil
}
