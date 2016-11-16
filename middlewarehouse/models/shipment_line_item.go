package models

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID      uint
	ReferenceNumber string
	StockItemUnitID uint
	StockItemUnit   StockItemUnit
	SkuID           uint
	SkuCode         string
	Name            string
	Price           uint
	ImagePath       string
}

func NewShipmentLineItemFromPayload(payload *payloads.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		ReferenceNumber: payload.ReferenceNumber,
		SkuID:           payload.SkuID,
		SkuCode:         payload.SkuCode,
		Name:            payload.Name,
		Price:           payload.Price,
		ImagePath:       payload.ImagePath,
	}
}

func NewShipmentLineItemFromOrderPayload(payload *payloads.OrderLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ReferenceNumber: payload.ReferenceNumber,
		SkuID:           payload.SkuID,
		SkuCode:         payload.SkuCode,
		Name:            payload.Name,
		Price:           payload.Price,
		ImagePath:       payload.ImagePath,
	}
}
