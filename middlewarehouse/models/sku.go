package models

import "github.com/FoxComm/highlander/middlewarehouse/common/gormfox"

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
