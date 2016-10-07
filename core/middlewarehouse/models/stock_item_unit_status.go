package models

import "database/sql/driver"

type UnitStatus string

type StatusChange struct {
	From UnitStatus
	To   UnitStatus
}

const (
	StatusOnHand   UnitStatus = "onHand"
	StatusOnHold   UnitStatus = "onHold"
	StatusReserved UnitStatus = "reserved"
	StatusShipped  UnitStatus = "shipped"
	StatusEmpty    UnitStatus = ""
)

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *UnitStatus) Scan(value interface{}) error {
	*u = UnitStatus(string(value.([]byte)))

	return nil
}
func (u UnitStatus) Value() (driver.Value, error) {
	return string(u), nil
}
