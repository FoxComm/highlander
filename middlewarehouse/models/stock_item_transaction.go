package models

import "time"

type StockItemTransaction struct {
	ID             uint
	StockItemID    uint
	Type           UnitType
	Status         UnitStatus
	QuantityNew    uint
	QuantityChange int
	AFSNew         uint
	CreatedAt      time.Time
}
