package models

import (
	"database/sql"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type Address struct {
	gormfox.Base
	Name        string
	RegionID    uint
	Region      Region
	City        string
	Zip         string
	Address1    string
	Address2    sql.NullString
	PhoneNumber string
}

func NewAddressFromPayload(payload *payloads.Address) *Address {
	return &Address{
		Name:        payload.Name,
		RegionID:    payload.Region.ID,
		Region:      *NewRegionFromPayload(&payload.Region),
		Address1:    payload.Address1,
		City:        payload.City,
		Zip:         payload.Zip,
		Address2:    utils.MakeSqlNullString(payload.Address2),
		PhoneNumber: payload.PhoneNumber,
	}
}
