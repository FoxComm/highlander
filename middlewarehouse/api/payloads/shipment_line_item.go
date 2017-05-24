package payloads

type ShipmentLineItem struct {
	ID               uint     `json:"id"`
	ReferenceNumbers []string `json:"referenceNumbers" binding:"required"`
	SKU              string   `json:"sku" binding:"required"`
	Name             string   `json:"name" binding:"required"`
	Price            uint     `json:"price" binding:"required"`
	ImagePath        string   `json:"imagePath" binding:"required"`
}
