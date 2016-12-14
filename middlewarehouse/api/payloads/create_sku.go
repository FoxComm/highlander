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
	ReturnWindow                  *dimension     `json:"returnWindow"`
	Height                        *dimension     `json:"height"`
	Weight                        *dimension     `json:"weight"`
	Length                        *dimension     `json:"length"`
	Width                         *dimension     `json:"width"`
	RequiresInventoryTracking     bool           `json:"requiresInventoryTracking"`
	InventoryWarningLevel         *QuantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart         *QuantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart         *QuantityLevel `json:"minimumQuantityInCart"`
	AllowPreorder                 bool           `json:"allowPreorder"`
	AllowBackorder                bool           `json:"allowBackorder"`
	RequiresLotTracking           bool           `json:"requiresLotTracking"`
	LotExpirationThreshold        *dimension     `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold *dimension     `json:"lotExpirationWarningThreshold"`
}

func (sku CreateSKU) Model() models.SKU {
	return models.SKU{
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

type dimension struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}

type QuantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
