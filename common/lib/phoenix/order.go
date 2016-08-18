package phoenix

// Order represents the order object that exists in the orders_search_view.
type Order struct {
	Totals          OrderTotals     `json:"totals" binding:"required"`
	Customer        Customer        `json:"customer" binding:"required"`
	PlacedAt        string          `json:"placedAt" binding:"required"`
	LineItems       OrderLineItems  `json:"lineItems" binding:"required"`
	FraudScore      int             `json:"fraudScore" binding:"required"`
	OrderState      string          `json:"orderState" binding:"required"`
	PaymentState    string          `json:"paymentState" binding:"required"`
	ShippingState   string          `json:"shippingState" binding:"required"`
	PaymentMethods  []PaymentMethod `json:"paymentMethods" binding:"required"`
	ShippingMethod  ShippingMethod  `json:"shippingMethod" binding:"required"`
	ReferenceNumber string          `json:"referenceNumber" binding:"required"`
	ShippingAddress Address         `json:"shippingAddress" binding:"required"`
	RemorseHoldEnd  *string         `json:"remorseHoldEnd"`
}
