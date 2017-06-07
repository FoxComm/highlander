package models

import (
	"database/sql/driver"
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type StringsArray []string

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID       uint
	ReferenceNumbers StringsArray
	StockItemUnitID  uint
	StockItemUnit    StockItemUnit
	SKU              string
	Name             string
	Price            uint
	ImagePath        string
}

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *StringsArray) Scan(value interface{}) error {
	*u = strings.Split(string(value.([]byte)), ",")

	return nil
}
func (u StringsArray) Value() (driver.Value, error) {
	return strings.Join(u, ","), nil
}
