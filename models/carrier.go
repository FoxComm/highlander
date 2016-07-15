package models

import (
	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type Carrier struct {
	ID               uint `json:"id"`
	Name             string
	TrackingTemplate string
}

func (carrier *Carrier) Identifier() uint {
	return carrier.ID
}

func NewCarrierFromPayload(payload *payloads.Carrier) *Carrier {
	return &Carrier{
		Name:             payload.Name,
		TrackingTemplate: payload.TrackingTemplate,
	}
}
