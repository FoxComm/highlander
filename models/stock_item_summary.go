package models

import "github.com/FoxComm/middlewarehouse/common/gormfox"

type StockItemSummary struct {
	gormfox.Base
	StockItemID uint
	OnHand      int
	OnHold      int
	Reserved    int
}

func (sis StockItemSummary) Identifier() uint {
	return sis.ID
}
