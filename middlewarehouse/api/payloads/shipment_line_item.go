package payloads

type ShipmentLineItem struct {
	ID               uint     `json:"id"`
	ReferenceNumbers []string `json:"referenceNumbers" binding:"required"`
	SkuID            uint     `json:"skuId" binding:"required"`
	SkuCode          string   `json:"skuCode" binding:"required"`
	Name             string   `json:"name" binding:"required"`
	Price            uint     `json:"price" binding:"required"`
	ImagePath        string   `json:"imagePath" binding:"required"`
}
