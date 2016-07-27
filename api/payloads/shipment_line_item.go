package payloads

type ShipmentLineItem struct {
	ReferenceNumber string `json:"referenceNumber" binding:"required"`
	SKU             string `json:"sku" binding:"required"`
	Name            string `json:"name" binding:"required"`
	Price           uint   `json:"price" binding:"required"`
	ImagePath       string `json:"imagePath" binding:"required"`
	State           string `json:"state" binding:"required"`
}
