package payloads

// OrderLineItem is the representation a SKU that is part of an order.
type OrderLineItem struct {
	SKU              string      `json:"sku" binding:"required"`
	Name             string      `json:"name" binding:"required"`
	Price            uint        `json:"price"`
	State            string      `json:"state" binding:"required"`
	ReferenceNumbers []string    `json:"referenceNumbers" binding:"required"`
	ImagePath        string      `json:"image_path" binding:"required"`
	Attributes       *Attributes `json:"attributes"`
	Quantity         int         `json:"quantity"`
	TrackInventory   bool        `json:"trackInventory"`
}

type OrderLineItems struct {
	SKUs []OrderLineItem `json:"skus"`
}

type UpdateOrderLineItem struct {
	State           string      `json:"state"`
	Attributes      *Attributes `json:"attributes"`
	ReferenceNumber string      `json:"referenceNumber"`
}

func NewUpdateOrderLineItem(lineItem OrderLineItem, refNum string) *UpdateOrderLineItem {
	return &UpdateOrderLineItem{
		State:           lineItem.State,
		Attributes:      lineItem.Attributes,
		ReferenceNumber: refNum,
	}
}
