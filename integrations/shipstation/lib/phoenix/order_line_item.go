package phoenix

// OrderLineItem is the representation a SKU that is part of an order.
type OrderLineItem struct {
	SKU             string `json:"sku"`
	Name            string
	Price           int
	State           string
	ReferenceNumber string `json:"reference_number"`
}
