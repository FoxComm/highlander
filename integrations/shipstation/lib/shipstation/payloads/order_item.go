package payloads

// OrderItem is a single line item in an order.
type OrderItem struct {
	LineItemKey       string
	SKU               string `json:"sku"`
	Name              string
	ImageURL          string `json:"imageUrl"`
	Weight            Weight
	Quantity          int
	UnitPrice         float64
	TaxAmount         float64
	ShippingAmount    float64
	WarehouseLocation string
	Options           []ItemOption
	ProductID         *int
	FulfillmentSKU    *string `json:"fulfillmentSku"`
	Adjustment        bool
	UPC               *string `json:"upc"`
}
