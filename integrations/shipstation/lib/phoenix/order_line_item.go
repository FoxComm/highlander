package phoenix

// OrderLineItem is the representation a SKU that is part of an order.
type OrderLineItem struct {
	SKU             string `json:"sku"`
	Name            string
	Price           uint
	State           string
	ReferenceNumber string `json:"reference_number"`
	ImagePath       string `json:"image_path"`
}
