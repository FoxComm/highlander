package models

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

const (
	ShippingTypeFlat = iota
	ShippingTypeVariable
)

type ShippingMethod struct {
	ID           uint
	CarrierID    uint
	Carrier      Carrier
	Name         string
	Code         string
	ShippingType int
	Cost         uint
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}

func NewShippingMethodFromPayload(payload *payloads.ShippingMethod) (*ShippingMethod, exceptions.IException) {
	sm := &ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
		Code:      payload.Code,
		Cost:      payload.Cost,
	}

	switch payload.ShippingType {
	case "flat":
		sm.ShippingType = ShippingTypeFlat
	case "variable":
		sm.ShippingType = ShippingTypeVariable
	default:
		return nil, exceptions.NewValidationException(
			fmt.Errorf("Expected shipping type flat or variable, got: %s", payload.ShippingType),
		)
	}

	return sm, nil
}
