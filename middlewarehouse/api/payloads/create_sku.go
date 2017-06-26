package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

type CreateSKU struct {
	Code                          string         `json:"code" binding:"required"`
	UPC                           string         `json:"upc"`
	Title                         string         `json:"title"`
	UnitCost                      *Money         `json:"unitCost"`
	TaxClass                      string         `json:"taxClass" binding:"required"`
	RequiresShipping              bool           `json:"requiresShipping"`
	ShippingClass                 string         `json:"shippingClass"`
	IsReturnable                  bool           `json:"isReturnable"`
	ReturnWindow                  *PhysicalUnit  `json:"returnWindow"`
	Height                        *PhysicalUnit  `json:"height"`
	Weight                        *PhysicalUnit  `json:"weight"`
	Length                        *PhysicalUnit  `json:"length"`
	Width                         *PhysicalUnit  `json:"width"`
	RequiresInventoryTracking     bool           `json:"requiresInventoryTracking"`
	InventoryWarningLevel         *QuantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart         *QuantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart         *QuantityLevel `json:"minimumQuantityInCart"`
	AllowPreorder                 bool           `json:"allowPreorder"`
	AllowBackorder                bool           `json:"allowBackorder"`
	RequiresLotTracking           bool           `json:"requiresLotTracking"`
	LotExpirationThreshold        *PhysicalUnit  `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold *PhysicalUnit  `json:"lotExpirationWarningThreshold"`
	Scopable
}

func (sku CreateSKU) Model() *models.SKU {
	model := &models.SKU{
		Scope:                     sku.Scope,
		Code:                      sku.Code,
		UPC:                       sku.UPC,
		Title:                     sku.Title,
		TaxClass:                  sku.TaxClass,
		RequiresShipping:          sku.RequiresShipping,
		ShippingClass:             sku.ShippingClass,
		IsReturnable:              sku.IsReturnable,
		RequiresInventoryTracking: sku.RequiresInventoryTracking,
		AllowBackorder:            sku.AllowBackorder,
		AllowPreorder:             sku.AllowPreorder,
		RequiresLotTracking:       sku.RequiresLotTracking,
	}

	if sku.UnitCost != nil {
		model.UnitCostCurrency = sku.UnitCost.Currency
		model.UnitCostValue = sku.UnitCost.Value
	} else {
		model.UnitCostCurrency = "USD"
		model.UnitCostValue = 0
	}

	if sku.ReturnWindow != nil {
		model.ReturnWindowValue = sku.ReturnWindow.Value
		model.ReturnWindowUnits = sku.ReturnWindow.Units
	}

	if sku.Height != nil {
		model.HeightValue = sku.Height.Value
		model.HeightUnits = sku.Height.Units
	}

	if sku.Weight != nil {
		model.WeightValue = sku.Weight.Value
		model.WeightUnits = sku.Weight.Units
	}

	if sku.Length != nil {
		model.LengthValue = sku.Length.Value
		model.LengthUnits = sku.Length.Units
	}

	if sku.Width != nil {
		model.WidthValue = sku.Width.Value
		model.WidthUnits = sku.Width.Units
	}

	if sku.LotExpirationThreshold != nil {
		model.LotExpirationThresholdValue = sku.LotExpirationThreshold.Value
		model.LotExpirationThresholdUnits = sku.LotExpirationThreshold.Units
	}

	if sku.LotExpirationWarningThreshold != nil {
		model.LotExpirationWarningThresholdValue = sku.LotExpirationWarningThreshold.Value
		model.LotExpirationWarningThresholdUnits = sku.LotExpirationWarningThreshold.Units
	}

	if sku.InventoryWarningLevel != nil {
		model.InventoryWarningLevelIsEnabled = sku.InventoryWarningLevel.IsEnabled
		model.InventoryWarningLevelValue = sku.InventoryWarningLevel.Level
	}

	if sku.MaximumQuantityInCart != nil {
		model.MaximumQuantityInCartIsEnabled = sku.MaximumQuantityInCart.IsEnabled
		model.MaximumQuantityInCartValue = sku.MaximumQuantityInCart.Level
	}

	if sku.MinimumQuantityInCart != nil {
		model.MinimumQuantityInCartIsEnabled = sku.MinimumQuantityInCart.IsEnabled
		model.MinimumQuantityInCartValue = sku.MinimumQuantityInCart.Level
	}

	return model
}
