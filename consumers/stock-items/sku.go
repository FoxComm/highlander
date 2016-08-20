package main

import (
	"encoding/json"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/api/payloads"
)

type SKU struct {
	ID   int    `json:"id" binding:"required"`
	Code string `json:"sku_code" binding:"required"`
}

func NewSKUFromAvro(message metamorphosis.AvroMessage) (*SKU, error) {
	s := new(SKU)
	if err := json.Unmarshal(message.Bytes(), s); err != nil {
		return nil, err
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
