package models

import "time"

type StockItemTransaction struct {
	ID           uint
	StockItemId  uint
	Type         UnitType
	Status       UnitStatus
	AmountNew    uint
	AmountChange int
	AFSNew       uint

	CreatedAt time.Time
}
