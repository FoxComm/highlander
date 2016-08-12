package models

import "database/sql/driver"

type ShipmentFailureReason string

const (
	ShipmentFailureReasonOutOfStock ShipmentFailureReason = "outOfStock"
)

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *ShipmentFailureReason) Scan(value interface{}) error {
	*u = ShipmentFailureReason(value.(string))

	return nil
}

func (u ShipmentFailureReason) Value() (driver.Value, error) {
	return string(u), nil
}
