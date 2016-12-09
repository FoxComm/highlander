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
	ReturnWindow Dimension `json:"returnWindow"`

	Height Dimension `json:"height"`
	Weight Dimension `json:"weight"`
	Length Dimension `json:"length"`
	Width  Dimension `json:"width"`

	RequiresInventoryTracking bool          `json:"requiresInventoryTracking"`
	InventoryWarningLevel     QuantityLevel `json:"inventoryWarningLevel"`
	MaximumQuantityInCart     QuantityLevel `json:"maximumQuantityInCart"`
	MinimumQuantityInCart     QuantityLevel `json:"minimumQuantityInCart"`

	AllowBackorder bool `json:"allowBackorder"`
	AllowPreorder  bool `json:"allowPreorder"`

	RequiresLotTracking           bool      `json:"requiresLotTracking"`
	LotExpirationThreshold        Dimension `json:"lotExpirationThreshold"`
	LotExpirationWarningThreshold Dimension `json:"lotExpirationWarningThreshold"`
}

func NewSKUFromModel(sku *models.SKU) *SKU {
	return &SKU{}
}

type Dimension struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}

type QuantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
