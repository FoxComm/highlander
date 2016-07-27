package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type ShippingMethod struct {
	ID        uint
	CarrierID uint
	Name      string
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}

func NewShippingMethodFromPayload(payload *payloads.ShippingMethod) *ShippingMethod {
	return &ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
	}
}
