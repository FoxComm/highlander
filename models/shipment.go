package models

import (
	"database/sql"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/utils"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type Shipment struct {
	gormfox.Base
	ShippingMethodCode string
	ShippingMethod     ShippingMethod `gorm:"ForeignKey:ShippingMethodCode"`
	ReferenceNumber    string
	State              ShipmentState
	ShipmentDate       sql.NullString
	EstimatedArrival   sql.NullString
	DeliveredDate      sql.NullString
	AddressID          uint
	Address            Address
	ShipmentLineItems  []ShipmentLineItem
	TrackingNumber     sql.NullString
}

func NewShipmentFromPayload(payload *payloads.Shipment) *Shipment {
	shipment := &Shipment{
		ShippingMethodCode: payload.ShippingMethodCode,
		ReferenceNumber:    payload.ReferenceNumber,
		State:              ShipmentState(payload.State),
		ShipmentDate:       utils.MakeSqlNullString(payload.ShipmentDate),
		EstimatedArrival:   utils.MakeSqlNullString(payload.EstimatedArrival),
		DeliveredDate:      utils.MakeSqlNullString(payload.DeliveredDate),
		AddressID:          payload.Address.ID,
		Address:            *NewAddressFromPayload(&payload.Address),
		TrackingNumber:     utils.MakeSqlNullString(payload.TrackingNumber),
	}

	for _, lineItem := range payload.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromPayload(&lineItem))
	}

	return shipment
}

func NewShipmentFromUpdatePayload(payload *payloads.UpdateShipment) *Shipment {
	shipment := new(Shipment)

	if payload.ShippingMethodCode != "" {
		shipment.ShippingMethodCode = payload.ShippingMethodCode
	}

	if payload.State != "" {
		shipment.State = ShipmentState(payload.State)
	}

	shipment.ShipmentDate = utils.MakeSqlNullString(payload.ShipmentDate)
	shipment.EstimatedArrival = utils.MakeSqlNullString(payload.EstimatedArrival)
	shipment.DeliveredDate = utils.MakeSqlNullString(payload.DeliveredDate)
	shipment.TrackingNumber = utils.MakeSqlNullString(payload.TrackingNumber)

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
		ShippingMethodCode: payload.ShippingMethod.Code,
		ReferenceNumber:    payload.ReferenceNumber,
		State:              ShipmentStatePending,
		Address:            *NewAddressFromPayload(&payload.ShippingAddress),
	}

	for _, lineItem := range payload.LineItems.SKUs {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromOrderPayload(&lineItem))
	}

	return shipment
}
