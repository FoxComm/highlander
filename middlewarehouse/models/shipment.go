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
	Scope              string
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
		Scope:              payload.Scope,
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

	shipment.Scope = payload.Scope
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
		for i := 0; i < lineItem.Quantity; i++ {
			shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromOrderPayload(&lineItem))
		}
	}

	shipment.Scope = payload.Scope

	return shipment
}

func (s Shipment) IsUpdated(other *Shipment) bool {
	address := s.Address
	otherAddress := other.Address

	return address.Name != otherAddress.Name ||
		address.RegionID != otherAddress.RegionID ||
		address.City != otherAddress.City ||
		address.Zip != otherAddress.Zip ||
		address.Address1 != otherAddress.Address1 ||
		!utils.CompareNullStrings(address.Address2, otherAddress.Address2) ||
		address.PhoneNumber != otherAddress.PhoneNumber ||
		s.ShippingMethodCode != other.ShippingMethodCode ||
		s.State != other.State ||
		!utils.CompareNullStrings(s.ShipmentDate, other.ShipmentDate) ||
		!utils.CompareNullStrings(s.EstimatedArrival, other.EstimatedArrival) ||
		!utils.CompareNullStrings(s.DeliveredDate, other.DeliveredDate) ||
		!utils.CompareNullStrings(s.TrackingNumber, other.TrackingNumber) ||
		s.ShippingPrice != other.ShippingPrice
}
