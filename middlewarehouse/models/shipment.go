package models

import (
	"database/sql"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type Shipment struct {
	gormfox.Base
	ReferenceNumber    string
	ShippingMethodCode string
	ShippingMethod     ShippingMethod `gorm:"ForeignKey:ShippingMethodCode"`
	OrderRefNum        string
	State              ShipmentState
	ShipmentDate       sql.NullString
	EstimatedArrival   sql.NullString
	DeliveredDate      sql.NullString
	AddressID          uint
	Address            Address
	ShipmentLineItems  []ShipmentLineItem
	TrackingNumber     sql.NullString
	ShippingPrice      int
}

func NewShipmentFromPayload(payload *payloads.Shipment) *Shipment {
	shipment := &Shipment{
		ShippingMethodCode: payload.ShippingMethodCode,
		OrderRefNum:        payload.OrderRefNum,
		State:              ShipmentState(payload.State),
		ShipmentDate:       utils.MakeSqlNullString(payload.ShipmentDate),
		EstimatedArrival:   utils.MakeSqlNullString(payload.EstimatedArrival),
		DeliveredDate:      utils.MakeSqlNullString(payload.DeliveredDate),
		AddressID:          payload.Address.ID,
		Address:            *NewAddressFromPayload(&payload.Address),
		TrackingNumber:     utils.MakeSqlNullString(payload.TrackingNumber),
		ShippingPrice:      payload.ShippingPrice,
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

	if payload.ShippingPrice != nil {
		shipment.ShippingPrice = *(payload.ShippingPrice)
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
		OrderRefNum:        payload.ReferenceNumber,
		State:              ShipmentStatePending,
		Address:            *NewAddressFromPayload(&payload.ShippingAddress),
		ShippingPrice:      payload.ShippingMethod.Price,
	}

    for _, lineItem := range payload.LineItems.SKUs {
        for i := 0 ; i < lineItem.Quantity; i++ {
            shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromOrderPayload(&lineItem))
        }
    }

	return shipment
}
