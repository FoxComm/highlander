package models

import "database/sql/driver"

type ShipmentState string

const (
	ShipmentStatePending   ShipmentState = "pending"
	ShipmentStateShipped   ShipmentState = "shipped"
	ShipmentStateDelivered ShipmentState = "delivered"
	ShipmentStateCancelled ShipmentState = "cancelled"
)

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *ShipmentState) Scan(value interface{}) error {
	*u = ShipmentState(value.(string))

	return nil
}

func (u ShipmentState) Value() (driver.Value, error) {
	return string(u), nil
}
