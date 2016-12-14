package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type CreateSKU struct {
	Code                          string         `json:"code" binding:"required"`
	UPC                           string         `json:"upc"`
	Title                         string         `json:"title"`
	UnitCost                      int            `json:"unitCost"`
	TaxClass                      string         `json:"taxClass" binding:"required"`
	RequiresShipping              bool           `json:"requiresShipping"`
	ShippingClass                 string         `json:"shippingClass"`
	IsReturnable                  bool           `json:"isReturnable"`
	ReturnWindow                  *Dimension     `json:"returnWindow"`
	Height                        *Dimension     `json:"height"`
	Weight                        *Dimension     `json:"weight"`
	Length                        *Dimension     `json:"length"`
	Width                         *Dimension     `json:"width"`
	RequiresInventoryTracking     bool           `json:"requiresInventoryTracking"`
	InventoryWarningLevel         *QuantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart         *QuantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart         *QuantityLevel `json:"minimumQuantityInCart"`
	AllowPreorder                 bool           `json:"allowPreorder"`
	AllowBackorder                bool           `json:"allowBackorder"`
	RequiresLotTracking           bool           `json:"requiresLotTracking"`
	LotExpirationThreshold        *Dimension     `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold *Dimension     `json:"lotExpirationWarningThreshold"`
	Scopable
}

func (sku CreateSKU) Model() *models.SKU {
	return &models.SKU{
		Scope:                              sku.Scope,
		Code:                               sku.Code,
		UPC:                                sku.UPC,
		Title:                              sku.Title,
		UnitCost:                           sku.UnitCost,
		TaxClass:                           sku.TaxClass,
		RequiresShipping:                   sku.RequiresShipping,
		IsReturnable:                       sku.IsReturnable,
		ReturnWindowValue:                  sku.ReturnWindow.Value,
		ReturnWindowUnits:                  sku.ReturnWindow.Units,
		HeightValue:                        sku.Height.Value,
		HeightUnits:                        sku.Height.Units,
		WeightValue:                        sku.Weight.Value,
		WeightUnits:                        sku.Weight.Units,
		LengthValue:                        sku.Length.Value,
		LengthUnits:                        sku.Length.Units,
		WidthValue:                         sku.Width.Value,
		WidthUnits:                         sku.Width.Units,
		RequiresInventoryTracking:          sku.RequiresInventoryTracking,
		InventoryWarningLevelIsEnabled:     sku.InventoryWarningLevel.IsEnabled,
		InventoryWarningLevelValue:         sku.InventoryWarningLevel.Level,
		MaximumQuantityInCartIsEnabled:     sku.MaximumQuantityInCart.IsEnabled,
		MaximumQuantityInCartValue:         sku.MaximumQuantityInCart.Level,
		MinimumQuantityInCartIsEnabled:     sku.MinimumQuantityInCart.IsEnabled,
		MinimumQuantityInCartValue:         sku.MinimumQuantityInCart.Level,
		AllowBackorder:                     sku.AllowBackorder,
		AllowPreorder:                      sku.AllowPreorder,
		RequiresLotTracking:                sku.RequiresLotTracking,
		LotExpirationThresholdValue:        sku.LotExpirationThreshold.Value,
		LotExpirationThresholdUnits:        sku.LotExpirationThreshold.Units,
		LotExpirationWarningThresholdValue: sku.LotExpirationWarningThreshold.Value,
		LotExpirationWarningThresholdUnits: sku.LotExpirationWarningThreshold.Units,
	}
}

type Dimension struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}

type QuantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
