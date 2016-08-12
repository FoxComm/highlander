package models

import (
	"database/sql"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
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
