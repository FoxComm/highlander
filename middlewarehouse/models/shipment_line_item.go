package models

import (
	"database/sql/driver"
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"strings"
)

type StringsArray []string

type ShipmentLineItem struct {
	gormfox.Base
	ShipmentID       uint
	ReferenceNumbers StringsArray
	StockItemUnitID  uint
	StockItemUnit    StockItemUnit
	SkuID            uint
	SkuCode          string
	Name             string
	Price            uint
	ImagePath        string
}

func NewShipmentLineItemFromPayload(payload *payloads.ShipmentLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		Base: gormfox.Base{
			ID: payload.ID,
		},
		ReferenceNumbers: payload.ReferenceNumbers,
		SkuID:            payload.SkuID,
		SkuCode:          payload.SkuCode,
		Name:             payload.Name,
		Price:            payload.Price,
		ImagePath:        payload.ImagePath,
	}
}

func NewShipmentLineItemFromOrderPayload(payload *payloads.OrderLineItem) *ShipmentLineItem {
	return &ShipmentLineItem{
		ReferenceNumbers: payload.ReferenceNumbers,
		SkuID:            payload.SkuID,
		SkuCode:          payload.SkuCode,
		Name:             payload.Name,
		Price:            payload.Price,
		ImagePath:        payload.ImagePath,
	}
}

// implement Scanner and Valuer interfaces to provide read/write for alias type
func (u *StringsArray) Scan(value interface{}) error {
	*u = strings.Split(string(value.([]byte)), ",")

	return nil
}
func (u StringsArray) Value() (driver.Value, error) {
	return strings.Join(u, ","), nil
}
