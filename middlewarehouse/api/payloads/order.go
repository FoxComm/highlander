package payloads

// Order represents the order object that exists in the orders_search_view.
type Order struct {
	Totals          OrderTotals          `json:"totals"`
	Customer        Customer             `json:"customer"`
	PlacedAt        string               `json:"placedAt"`
	LineItems       OrderLineItems       `json:"lineItems"`
	FraudScore      int                  `json:"fraudScore"`
	OrderState      string               `json:"orderState"`
	PaymentState    string               `json:"paymentState"`
	ShippingState   string               `json:"shippingState"`
	PaymentMethods  []PaymentMethod      `json:"paymentMethods"`
	ShippingMethod  *OrderShippingMethod `json:"shippingMethod"`
	ReferenceNumber string               `json:"referenceNumber"`
	ShippingAddress *Address             `json:"shippingAddress"`
	RemorseHoldEnd  *string              `json:"remorseHoldEnd"`
	Scopable
}

func (order *Order) SetScope(scope string) {
	order.Scope = scope

	order.ShippingMethod.SetScope(scope)
}

// Order wrapped in Phoenix response
type OrderResult struct {
	Order Order `json:"result" binding:"required"`
}
