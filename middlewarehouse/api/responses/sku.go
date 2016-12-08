package responses

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

type dimension struct {
	Value float64 `json:"value"`
	Units string  `json:"units"`
}

type quantityLevel struct {
	IsEnabled bool `json:"isEnabled"`
	Level     int  `json:"level"`
}
