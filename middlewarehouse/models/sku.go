package models

import (
	ce "github.com/FoxComm/highlander/middlewarehouse/common/errors"
	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
)

type SKU struct {
	gormfox.Base
	Scope                              string
	Code                               string
	UPC                                string
	Title                              string
	UnitCost                           int
	TaxClass                           string
	RequiresShipping                   bool
	ShippingClass                      string
	IsReturnable                       bool
	ReturnWindowValue                  float64
	ReturnWindowUnits                  string
	HeightValue                        float64
	HeightUnits                        string
	WeightValue                        float64
	WeightUnits                        string
	LengthValue                        float64
	LengthUnits                        string
	WidthValue                         float64
	WidthUnits                         string
	RequiresInventoryTracking          bool
	InventoryWarningLevelIsEnabled     bool
	InventoryWarningLevelValue         int
	MaximumQuantityInCartValue         int
	MaximumQuantityInCartIsEnabled     bool
	MinimumQuantityInCartValue         int
	MinimumQuantityInCartIsEnabled     bool
	AllowBackorder                     bool
	AllowPreorder                      bool
	RequiresLotTracking                bool
	LotExpirationThresholdValue        float64
	LotExpirationThresholdUnits        string
	LotExpirationWarningThresholdValue float64
	LotExpirationWarningThresholdUnits string
}

func (s *SKU) Validate() error {
	v := ce.NewValidation("sku")

	v.NonEmpty(s.Code, "code")
	v.NonEmpty(s.TaxClass, "tax_class")

	if s.RequiresShipping {
		v.NonEmpty(s.ShippingClass, "shipping_class", "shipping is required")
	}

	if s.IsReturnable {
		v.GreaterThanF(0.0, s.ReturnWindowValue, "return_window.value", "an item is returnable")
		v.NonEmpty(s.ReturnWindowUnits, "return_window.units", "an item is returnable")
	}

	return v.ToError()
}
