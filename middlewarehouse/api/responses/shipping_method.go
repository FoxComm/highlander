package responses

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type ShippingMethod struct {
	ID           uint    `json:"id"`
	Carrier      Carrier `json:"carrier"`
	Name         string  `json:"name"`
	Code         string  `json:"code"`
	ShippingType string  `json:"type"`
	Cost         uint    `json:"cost"`
}

func NewShippingMethodFromModel(shippingMethod *models.ShippingMethod) (*ShippingMethod, error) {
	sm := &ShippingMethod{
		ID:      shippingMethod.ID,
		Carrier: *NewCarrierFromModel(&shippingMethod.Carrier),
		Name:    shippingMethod.Name,
		Code:    shippingMethod.Code,
		Cost:    shippingMethod.ExpectedCost,
	}

	if shippingMethod.ShippingType == models.ShippingTypeFlat {
		sm.ShippingType = "flat"
	} else if shippingMethod.ShippingType == models.ShippingTypeVariable {
		sm.ShippingType = "variable"
	} else {
		return nil, errors.New("Unexpected shipping type")
	}

	return sm, nil
}
