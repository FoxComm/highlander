package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
)

type Carrier struct {
	ID               uint
	Name             string
	TrackingTemplate string
	Scope            string
}

func (carrier *Carrier) Identifier() uint {
	return carrier.ID
}

func NewCarrierFromPayload(payload *payloads.Carrier) *Carrier {
	return &Carrier{
		Name:             payload.Name,
		TrackingTemplate: payload.TrackingTemplate,
		Scope:            payload.Scope,
	}
}
