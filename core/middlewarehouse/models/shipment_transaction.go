package models

import (
	"time"
)

const (
	TransactionCreditCard  = "creditCard"
	TransactionGiftCard    = "giftCard"
	TransactionStoreCredit = "storeCredit"
)

type ShipmentTransaction struct {
	ID         uint
	ShipmentID uint
	Type       string
	Source     string
	CreatedAt  time.Time
	Amount     uint
}
