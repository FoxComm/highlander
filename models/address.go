package models

import (
	"database/sql"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
)

type Address struct {
	gormfox.Base
	Name        string
	Address1    string
	Address2    sql.NullString
	City        string
	RegionID    int
	Zip         string
	PhoneNumber string
}
