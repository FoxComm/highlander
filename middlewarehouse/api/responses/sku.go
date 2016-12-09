package responses

import "github.com/FoxComm/highlander/middlewarehouse/models"

type SKU struct {
	ID    uint   `json:"id"`
	Code  string `json:"code"`
	UPC   string `json:"upc"`
	Title string `json:"title"`

	UnitCost int    `json:"unitCost"`
	TaxClass string `json:"taxClass"`

	RequiresShipping bool   `json:"requiresShipping"`
	ShippingClass    string `json:"shippingClass"`

	IsReturnable bool      `json:"isReturnable"`
	ReturnWindow dimension `json:"returnWindow"`

	Height dimension `json:"height"`
	Weight dimension `json:"weight"`
	Length dimension `json:"length"`
	Width  dimension `json:"width"`

	RequiresInventoryTracking bool          `json:"requiresInventoryTracking"`
	InventoryWarningLevel     quantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart     quantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart     quantityLevel `json:"minimumQuantityInCart"`

	AllowBackorder bool `json:"allowBackorder"`
	AllowPreorder  bool `json:"allowPreorder"`

	RequiresLotTracking           bool      `json:"requiresLotTracking"`
	LotExpirationThreshold        dimension `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold dimension `json:"lotExpirationWarningThreshold"`
}

func NewSKUFromModel(sku *models.SKU) *SKU {
	return &SKU{
		Code:             sku.Code,
		UPC:              sku.UPC,
		Title:            sku.Title,
		UnitCost:         sku.UnitCost,
		TaxClass:         sku.TaxClass,
		RequiresShipping: sku.RequiresShipping,
		ShippingClass:    sku.ShippingClass,
		IsReturnable:     sku.IsReturnable,
		ReturnWindow: dimension{
			Value: sku.ReturnWindowValue,
			Units: sku.ReturnWindowUnits,
		},
		Height: dimension{
			Value: sku.HeightValue,
			Units: sku.HeightUnits,
		},
		Weight: dimension{
			Value: sku.WeightValue,
			Units: sku.WeightUnits,
		},
		Length: dimension{
			Value: sku.LengthValue,
			Units: sku.LengthUnits,
		},
		Width: dimension{
			Value: sku.WidthValue,
			Units: sku.WidthUnits,
		},
		RequiresInventoryTracking: sku.RequiresInventoryTracking,
		InventoryWarningLevel: quantityLevel{
			IsEnabled: sku.InventoryWarningLevelIsEnabled,
			Level:     sku.InventoryWarningLevelValue,
		},
		MaximumQuantityInCart: quantityLevel{
			IsEnabled: sku.MaximumQuantityInCartIsEnabled,
			Level:     sku.MaximumQuantityInCartValue,
		},
		MinimumQuantityInCart: quantityLevel{
			IsEnabled: sku.MinimumQuantityInCartIsEnabled,
			Level:     sku.MinimumQuantityInCartValue,
		},
		AllowBackorder:      sku.AllowBackorder,
		AllowPreorder:       sku.AllowPreorder,
		RequiresLotTracking: sku.RequiresLotTracking,
		LotExpirationThreshold: dimension{
			Value: sku.LotExpirationThresholdValue,
			Units: sku.LotExpirationThresholdUnits,
		},
		LotExpirationWarningThreshold: dimension{
			Value: sku.LotExpirationWarningThresholdValue,
			Units: sku.LotExpirationWarningThresholdUnits,
		},
	}
}

type dimension struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}

type quantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
