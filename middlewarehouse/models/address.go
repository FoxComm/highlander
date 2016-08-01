package models

import (
	"database/sql"
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type Address struct {
	gormfox.Base
	Name        string
	RegionID    uint
	City        string
	Zip         string
	Address1    string
	Address2    sql.NullString
	PhoneNumber string
}

func NewAddressFromPayload(payload *payloads.Address) *Address {
	address := new(Address)

	address.Name = payload.Name
	address.RegionID = payload.RegionID
	address.Address1 = payload.Address1
	address.City = payload.City
	address.Zip = payload.Zip
	if payload.Address2 != "" {
		address.Address2 = sql.NullString{payload.Address2, true}
	}
	address.PhoneNumber = payload.PhoneNumber

	return address
}
