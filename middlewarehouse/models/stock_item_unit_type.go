package models

import "database/sql/driver"

type UnitType string

const (
	Sellable    UnitType = "Sellable"
	NonSellable UnitType = "Non-sellable"
	Backorder   UnitType = "Backorder"
	Preorder    UnitType = "Preorder"
)

const NumberOfUnitTypes = 4

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *UnitType) Scan(value interface{}) error {
	*u = UnitType(string(value.([]byte)))

	return nil
}
func (u UnitType) Value() (driver.Value, error) {
	return string(u), nil
}
