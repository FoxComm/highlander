package payloads

type UpdateShipment struct {
	ShippingMethodID  uint               `json:"shippingMethodId"`
	State             *string            `json:"state""`
	ShipmentDate      *string            `json:"shipmentDate"`
	EstimatedArrival  *string            `json:"estimatedArrival"`
	DeliveredDate     *string            `json:"deliveredDate"`
	Address           *Address           `json:"address"`
	ShipmentLineItems []ShipmentLineItem `json:"lineItems"`
	TrackingNumber    *string            `json:"trackingNumber"`
}
