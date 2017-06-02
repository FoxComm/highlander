package payloads

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type ShippingMethod struct {
	CarrierID    uint   `json:"carrierId"`
	Name         string `json:"name"`
	Code         string `json:"code"`
	ShippingType string `json:"type"`
	Cost         uint   `json:"cost"`
	Scopable
}

func (payload *ShippingMethod) Model() (*models.ShippingMethod, error) {
	sm := &models.ShippingMethod{
		CarrierID: payload.CarrierID,
		Name:      payload.Name,
		Code:      payload.Code,
		Cost:      payload.Cost,
		Scope:     payload.Scope,
	}

	switch payload.ShippingType {
	case "flat":
		sm.ShippingType = models.ShippingTypeFlat
	case "variable":
		sm.ShippingType = models.ShippingTypeVariable
	default:
		return nil, fmt.Errorf("Expected shipping type flat or variable, got: %s", payload.ShippingType)
	}

	return sm, nil
}
