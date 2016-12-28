package payloads

type ShippingMethod struct {
	CarrierID    uint   `json:"carrierId"`
	Name         string `json:"name"`
	Code         string `json:"code"`
	ShippingType string `json:"type"`
	Cost         uint   `json:"cost"`
	Scopable
}
