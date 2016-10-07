package models

import "time"

type StockItemTransaction struct {
	ID             uint
	StockItemId    uint
	Type           UnitType
	Status         UnitStatus
	QuantityNew    uint
	QuantityChange int
	AFSNew         uint
	CreatedAt      time.Time
}
