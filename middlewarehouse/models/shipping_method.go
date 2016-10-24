package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
)

type ShippingMethod struct {
	ID        uint
	CarrierID uint
	Carrier   Carrier
	Name      string
	Code      string
	Scope     string
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}

func NewShippingMethodFromPayload(payload *payloads.ShippingMethod) *ShippingMethod {
	return &ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
		Code:      payload.Code,
		Scope:     payload.Scope,
	}
}
