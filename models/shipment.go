package models

import (
	"database/sql"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type Shipment struct {
	gormfox.Base
	ShippingMethodID  uint
	ShippingMethod    ShippingMethod
	ReferenceNumber   string
	State             ShipmentState
	ShipmentDate      sql.NullString
	EstimatedArrival  sql.NullString
	DeliveredDate     sql.NullString
	AddressID         uint
	Address           Address
	ShipmentLineItems []ShipmentLineItem
	TrackingNumber    sql.NullString
}

func NewShipmentFromPayload(payload *payloads.Shipment) *Shipment {
	shipment := &Shipment{
		ShippingMethodID: payload.ShippingMethodID,
		ReferenceNumber:  payload.ReferenceNumber,
		State:            ShipmentState(payload.State),
		ShipmentDate:     NewSqlNullStringFromString(payload.ShipmentDate),
		EstimatedArrival: NewSqlNullStringFromString(payload.EstimatedArrival),
		DeliveredDate:    NewSqlNullStringFromString(payload.DeliveredDate),
		AddressID:        payload.Address.ID,
		Address:          *NewAddressFromPayload(&payload.Address),
		TrackingNumber:   NewSqlNullStringFromString(payload.TrackingNumber),
	}

	for _, lineItem := range payload.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromPayload(&lineItem))
	}

	return shipment
}

func NewShipmentFromUpdatePayload(payload *payloads.UpdateShipment) *Shipment {
	shipment := new(Shipment)

	if payload.ShippingMethodID != 0 {
		shipment.ShippingMethodID = payload.ShippingMethodID
	}

	if payload.State != "" {
		shipment.State = ShipmentState(payload.State)
	}

	shipment.ShipmentDate = NewSqlNullStringFromString(payload.ShipmentDate)
	shipment.EstimatedArrival = NewSqlNullStringFromString(payload.EstimatedArrival)
	shipment.DeliveredDate = NewSqlNullStringFromString(payload.DeliveredDate)
	shipment.TrackingNumber = NewSqlNullStringFromString(payload.TrackingNumber)

	if payload.Address != nil {
		shipment.AddressID = payload.Address.ID
		shipment.Address = *NewAddressFromPayload(payload.Address)
	}

	for _, lineItem := range payload.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromPayload(&lineItem))
	}

	return shipment
}

func NewShipmentFromOrderPayload(payload *payloads.Order) *Shipment {
	shipment := &Shipment{
		ShippingMethodID: payload.ShippingMethod.ID,
		ReferenceNumber:  payload.ReferenceNumber,
		State:            ShipmentStatePending,
		Address:          *NewAddressFromPayload(&payload.ShippingAddress),
	}

	for _, lineItem := range payload.LineItems.SKUs {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromOrderPayload(&lineItem))
	}

	return shipment
}
