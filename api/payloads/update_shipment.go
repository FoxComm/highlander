package payloads

type UpdateShipment struct {
	ShippingMethodCode string             `json:"shippingMethodCode"`
	State              string             `json:"state""`
	ShipmentDate       *string            `json:"shipmentDate"`
	EstimatedArrival   *string            `json:"estimatedArrival"`
	DeliveredDate      *string            `json:"deliveredDate"`
	Address            *Address           `json:"address"`
	ShipmentLineItems  []ShipmentLineItem `json:"lineItems"`
	TrackingNumber     *string            `json:"trackingNumber"`
	ShippingPrice      *int               `json:"shippingPrice"`
}
