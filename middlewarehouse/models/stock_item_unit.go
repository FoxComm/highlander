package models

import (
	"database/sql"

	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StockItemUnit struct {
	gormfox.Base
	StockItemID         uint
	StockItem           StockItem
	Type                UnitType
	OrderRefNum         sql.NullString
	OrderLineItemRefNum sql.NullString
	UnitCost            int
	Status              UnitStatus
}

func (siu StockItemUnit) Identifier() uint {
	return siu.ID
}
