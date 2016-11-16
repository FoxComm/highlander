package main

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/metamorphosis"
)

type SKU struct {
	ID   uint   `json:"sku_id" binding:"required"`
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
		SkuID: 	         s.ID,
		SkuCode:         s.Code,
		StockLocationID: stockLocationID,
		DefaultUnitCost: 0,
	}
}
