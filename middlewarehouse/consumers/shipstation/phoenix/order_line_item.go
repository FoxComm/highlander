package phoenix

// OrderLineItem is the representation a SKU that is part of an order.
type OrderLineItem struct {
	SKU             string `json:"sku" binding:"required"`
	Name            string `json:"name" binding:"required"`
	Price           uint   `json:"price" binding:"required"`
	State           string `json:"state" binding:"required"`
	ReferenceNumber string `json:"referenceNumber" binding:"required"`
	ImagePath       string `json:"image_path" binding:"required"`
	Quantity        int    `json:"quantity" binding:"required"`
}

type OrderLineItems struct {
	SKUs []OrderLineItem `json:"skus" binding:"required"`
}
