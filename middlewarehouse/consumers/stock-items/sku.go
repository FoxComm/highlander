package main

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/metamorphosis"
)

type SKU struct {
	ID   int    `json:"id" binding:"required"`
	Code string `json:"sku_code" binding:"required"`
}

func NewSKUFromAvro(message metamorphosis.AvroMessage) (*SKU, exceptions.IException) {
	s := new(SKU)
	if err := json.Unmarshal(message.Bytes(), s); err != nil {
		return nil, consumers.NewHttpException(err)
	}

	return s, nil
}

func (s SKU) StockItem(stockLocationID uint) payloads.StockItem {
	return payloads.StockItem{
		SKU:             s.Code,
		StockLocationID: stockLocationID,
		DefaultUnitCost: 0,
	}
}
