package models

import (
	"database/sql"

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
