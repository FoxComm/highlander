package models

import (
	"database/sql"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type Shipment struct {
	gormfox.Base
	ShippingMethodID uint
	ReferenceNumber  string
	State            string
	ShipmentDate     sql.NullString
	EstimatedArrival sql.NullString
	DeliveredDate    sql.NullString
	AddressID        uint
	TrackingNumber   sql.NullString
}

func NewShipmentFromPayload(payload *payloads.Shipment) *Shipment {
	shipment := &Shipment{
		ShippingMethodID: payload.ShippingMethodID,
		ReferenceNumber:  payload.ReferenceNumber,
		State:            payload.State,
	}

	if payload.ShipmentDate != "" {
		shipment.ShipmentDate = sql.NullString{String: payload.ShipmentDate, Valid: true}
	}

	if payload.EstimatedArrival != "" {
		shipment.EstimatedArrival = sql.NullString{String: payload.EstimatedArrival, Valid: true}
	}

	if payload.DeliveredDate != "" {
		shipment.DeliveredDate = sql.NullString{String: payload.DeliveredDate, Valid: true}
	}

	return shipment
}
