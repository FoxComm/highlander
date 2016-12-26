package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type UpdateSKU struct {
	Code                          *string        `json:"code"`
	UPC                           *string        `json:"upc"`
	Title                         *string        `json:"title"`
	UnitCost                      *Money         `json:"unitCost"`
	TaxClass                      *string        `json:"taxClass"`
	RequiresShipping              *bool          `json:"requiresShipping"`
	ShippingClass                 *string        `json:"shippingClass"`
	IsReturnable                  *bool          `json:"isReturnable"`
	ReturnWindow                  *PhysicalUnit  `json:"returnWindow"`
	Height                        *PhysicalUnit  `json:"height"`
	Weight                        *PhysicalUnit  `json:"weight"`
	Length                        *PhysicalUnit  `json:"length"`
	Width                         *PhysicalUnit  `json:"width"`
	RequiresInventoryTracking     *bool          `json:"requiresInventoryTracking"`
	InventoryWarningLevel         *QuantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart         *QuantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart         *QuantityLevel `json:"minimumQuantityInCart"`
	AllowPreorder                 *bool          `json:"allowPreorder"`
	AllowBackorder                *bool          `json:"allowBackorder"`
	RequiresLotTracking           *bool          `json:"requiresLotTracking"`
	LotExpirationThreshold        *PhysicalUnit  `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold *PhysicalUnit  `json:"lotExpirationWarningThreshold"`
	Scopable
}

func (sku UpdateSKU) Model(original *models.SKU) *models.SKU {
	model := new(models.SKU)

	if sku.Code != nil {
		model.Code = *sku.Code
	} else {
		model.Code = original.Code
	}

	if sku.UPC != nil {
		model.UPC = *sku.UPC
	} else {
		model.UPC = original.UPC
	}

	if sku.Title != nil {
		model.Title = *sku.Title
	} else {
		model.Title = original.Title
	}

	if sku.UnitCost != nil {
		model.UnitCostCurrency = sku.UnitCost.Currency
		model.UnitCostValue = sku.UnitCost.Value
	} else {
		model.UnitCostCurrency = original.UnitCostCurrency
		model.UnitCostValue = original.UnitCostValue
	}

	if sku.TaxClass != nil {
		model.TaxClass = *sku.TaxClass
	} else {
		model.TaxClass = original.TaxClass
	}

	if sku.RequiresShipping != nil {
		model.RequiresShipping = *sku.RequiresShipping
	} else {
		model.RequiresShipping = original.RequiresShipping
	}

	if sku.ShippingClass != nil {
		model.ShippingClass = *sku.ShippingClass
	} else {
		model.ShippingClass = original.ShippingClass
	}

	if sku.IsReturnable != nil {
		model.IsReturnable = *sku.IsReturnable
	} else {
		model.IsReturnable = original.IsReturnable
	}

	if sku.RequiresInventoryTracking != nil {
		model.RequiresInventoryTracking = *sku.RequiresInventoryTracking
	} else {
		model.RequiresInventoryTracking = original.RequiresInventoryTracking
	}

	if sku.AllowPreorder != nil {
		model.AllowPreorder = *sku.AllowPreorder
	} else {
		model.AllowPreorder = original.AllowPreorder
	}

	if sku.AllowBackorder != nil {
		model.AllowBackorder = *sku.AllowBackorder
	} else {
		model.AllowBackorder = original.AllowBackorder
	}

	if sku.RequiresLotTracking != nil {
		model.RequiresLotTracking = *sku.RequiresLotTracking
	} else {
		model.RequiresLotTracking = original.RequiresLotTracking
	}

	if sku.ReturnWindow != nil {
		model.ReturnWindowValue = sku.ReturnWindow.Value
		model.ReturnWindowUnits = sku.ReturnWindow.Units
	} else {
		model.ReturnWindowValue = original.ReturnWindowValue
		model.ReturnWindowUnits = original.ReturnWindowUnits
	}

	if sku.Height != nil {
		model.HeightValue = sku.Height.Value
		model.HeightUnits = sku.Height.Units
	} else {
		model.HeightValue = original.HeightValue
		model.HeightUnits = original.HeightUnits
	}

	if sku.Weight != nil {
		model.WeightValue = sku.Weight.Value
		model.WeightUnits = sku.Weight.Units
	} else {
		model.WeightValue = original.WeightValue
		model.WeightUnits = original.WeightUnits
	}

	if sku.Length != nil {
		model.LengthValue = sku.Length.Value
		model.LengthUnits = sku.Length.Units
	} else {
		model.LengthValue = original.LengthValue
		model.LengthUnits = original.LengthUnits
	}

	if sku.Width != nil {
		model.WidthValue = sku.Width.Value
		model.WidthUnits = sku.Width.Units
	} else {
		model.WidthValue = original.WidthValue
		model.WidthUnits = original.WidthUnits
	}

	if sku.LotExpirationThreshold != nil {
		model.LotExpirationThresholdValue = sku.LotExpirationThreshold.Value
		model.LotExpirationThresholdUnits = sku.LotExpirationThreshold.Units
	} else {
		model.LotExpirationThresholdValue = original.LotExpirationThresholdValue
		model.LotExpirationThresholdUnits = original.LotExpirationThresholdUnits
	}

	if sku.LotExpirationWarningThreshold != nil {
		model.LotExpirationWarningThresholdValue = sku.LotExpirationWarningThreshold.Value
		model.LotExpirationWarningThresholdUnits = sku.LotExpirationWarningThreshold.Units
	} else {
		model.LotExpirationWarningThresholdValue = original.LotExpirationWarningThresholdValue
		model.LotExpirationWarningThresholdUnits = original.LotExpirationWarningThresholdUnits
	}

	if sku.InventoryWarningLevel != nil {
		model.InventoryWarningLevelIsEnabled = sku.InventoryWarningLevel.IsEnabled
		model.InventoryWarningLevelValue = sku.InventoryWarningLevel.Level
	} else {
		model.InventoryWarningLevelIsEnabled = original.InventoryWarningLevelIsEnabled
		model.InventoryWarningLevelValue = original.InventoryWarningLevelValue
	}

	if sku.MaximumQuantityInCart != nil {
		model.MaximumQuantityInCartIsEnabled = sku.MaximumQuantityInCart.IsEnabled
		model.MaximumQuantityInCartValue = sku.MaximumQuantityInCart.Level
	} else {
		model.MaximumQuantityInCartIsEnabled = original.MaximumQuantityInCartIsEnabled
		model.MaximumQuantityInCartValue = original.MaximumQuantityInCartValue
	}

	if sku.MinimumQuantityInCart != nil {
		model.MinimumQuantityInCartIsEnabled = sku.MinimumQuantityInCart.IsEnabled
		model.MinimumQuantityInCartValue = sku.MinimumQuantityInCart.Level
	} else {
		model.MinimumQuantityInCartIsEnabled = original.MinimumQuantityInCartIsEnabled
		model.MinimumQuantityInCartValue = original.MinimumQuantityInCartValue
	}

	return model
}
