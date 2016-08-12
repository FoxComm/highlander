package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID      uint
	ReferenceNumber string
	SKU             string
	Name            string
	Price           uint
	ImagePath       string
	State           ShipmentState
}

func NewShipmentLineItemFromPayload(payload *payloads.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		ReferenceNumber: payload.ReferenceNumber,
		SKU:             payload.SKU,
		Name:            payload.Name,
		Price:           payload.Price,
		ImagePath:       payload.ImagePath,
		State:           ShipmentState(payload.State),
	}
}
