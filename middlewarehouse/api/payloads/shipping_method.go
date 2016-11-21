package payloads

type ShippingMethod struct {
	CarrierID    uint   `json:"carrierId" binding:"required"`
	Name         string `json:"name" binding:"required"`
	Code         string `json:"code" binding:"required"`
	ShippingType string `json:"type" binding:"required"`
	ExpectedCost uint   `json:"expectedCost"`
}
