package models

import "github.com/FoxComm/middlewarehouse/common/gormfox"

type StockItemSummary struct {
	gormfox.Base
	SKU             string `gorm:"-"`
	StockItemID     uint
	StockLocationID uint
	TypeID          uint
	OnHand          int
	OnHold          int
	Reserved        int
	Shipped         int
}

func (sis StockItemSummary) Identifier() uint {
	return sis.ID
}
