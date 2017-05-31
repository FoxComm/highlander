package main

import (
	"encoding/json"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/metamorphosis"
)

type SKU struct {
	ID                  int       `json:"id" binding:"required"`
	Code                string    `json:"sku_code" binding:"required"`
	Context             string    `json:"context" binding:"required"`
	ContextId           int       `json:"context_id" binding:"required"`
	Title               string    `json:"title"`
	Image               string    `json:"image"`
	SalePrice           string    `json:"sale_price"`
	SalePriceCurrency   string    `json:"sale_price_currency"`
	ArchivedAt          time.Time `json:"archived_at"`
	RetailPrice         string    `json:"retail_price"`
	RetailPriceCurrency string    `json:"reatail_price_currency"`
	ExternalId          string    `json:"external_id"`
	Scope               string    `json:"scope" binding:"required"`
}

func NewSKUFromAvro(message metamorphosis.AvroMessage) (*SKU, error) {
	s := new(SKU)
	if err := json.Unmarshal(message.Bytes(), s); err != nil {
		return nil, err
	}

	return s, nil
}

func (s SKU) CreateSKU() payloads.CreateSKU {
	return payloads.CreateSKU{
		Code:             s.Code,
		Title:            s.Title,
		Scopable:         payloads.Scopable{Scope: s.Scope},
		RequiresShipping: true,
		ShippingClass:    "default",
		TaxClass:         "default",
	}
}
