package models

import (
	"database/sql"
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type Address struct {
	gormfox.Base
	Name        string
	Region      string
	Country     string
	City        string
	Zip         string
	Address1    string
	Address2    sql.NullString
	PhoneNumber string
}

func NewAddressFromPayload(payload *payloads.Address) *Address {
	return &Address{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		Name:        payload.Name,
		Region:      payload.Region,
		Country:     payload.Country,
		Address1:    payload.Address1,
		City:        payload.City,
		Zip:         payload.Zip,
		Address2:    NewSqlNullStringFromString(payload.Address2),
		PhoneNumber: payload.PhoneNumber,
	}
}
