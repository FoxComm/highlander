package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID       uint
	ReferenceNumbers []string
	StockItemUnitID  uint
	StockItemUnit    StockItemUnit
	SKU              string
	Name             string
	Price            uint
	ImagePath        string
}

func NewShipmentLineItemFromPayload(payload *payloads.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		ReferenceNumbers: payload.ReferenceNumbers,
		SKU:              payload.SKU,
		Name:             payload.Name,
		Price:            payload.Price,
		ImagePath:        payload.ImagePath,
	}
}

func NewShipmentLineItemFromOrderPayload(payload *payloads.OrderLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ReferenceNumbers: payload.ReferenceNumbers,
		SKU:              payload.SKU,
		Name:             payload.Name,
		Price:            payload.Price,
		ImagePath:        payload.ImagePath,
	}
}
