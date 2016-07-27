package payloads

type ShipmentMethod struct {
	CarrierID uint   `json:"carrierId" binding:"required"`
	Name      string `json:"name" binding:"required"`
}
