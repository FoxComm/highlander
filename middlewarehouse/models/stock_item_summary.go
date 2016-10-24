package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

type StockItemSummary struct {
	gormfox.Base
	StockItem   StockItem
	StockItemID uint
	Type        UnitType
	OnHand      int
	OnHold      int
	Reserved    int
	Shipped     int
	AFS         int
	AFSCost     int
	Scope       string
}

func (sis StockItemSummary) Identifier() uint {
	return sis.ID
}
