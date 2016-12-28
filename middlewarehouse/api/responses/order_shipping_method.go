package responses

// OrderShippingMethod is the response given when describing an applicable
// shipping method for an order.
type OrderShippingMethod struct {
	IsEnabled        bool  `json:"isEnabled"`
	ShippingMethodID uint  `json:"shippingMethodId"`
	Price            Money `json:"price"`
}
