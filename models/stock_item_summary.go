package models

import "github.com/FoxComm/middlewarehouse/common/gormfox"

type StockItemSummary struct {
	gormfox.Base
	StockItemID       uint
	SKU               string   `gorm:"-"`
	StockLocationID   uint     `gorm:"-"`
	StockLocationName string   `gorm:"-"`
	Type              UnitType `gorm:"type:text"`
	OnHand            int
	OnHold            int
	Reserved          int
	Shipped           int
	AFS               int
	AFSCost           int
}

func (sis StockItemSummary) Identifier() uint {
	return sis.ID
}
