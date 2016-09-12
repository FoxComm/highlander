package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type ShippingMethod struct {
	ID        uint
	CarrierID uint
	Carrier   Carrier
	Name      string
	Code      string
}

func (shippingMethod *ShippingMethod) Identifier() uint {
	return shippingMethod.ID
}

func NewShippingMethodFromPayload(payload *payloads.ShippingMethod) *ShippingMethod {
	return &ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
		Code:      payload.Code,
	}
}
