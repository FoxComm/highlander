package models

import (
	"database/sql"

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
