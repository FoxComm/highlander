package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

type StockItem struct {
	gormfox.Base
	SKU             string
	StockLocation   StockLocation
	StockLocationID uint
	DefaultUnitCost int
}

func (si StockItem) Identifier() uint {
	return si.ID
}
