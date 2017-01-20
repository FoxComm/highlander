package payloads

type Shipment struct {
	ShippingMethodCode string             `json:"shippingMethodCode" binding:"required"`
	OrderRefNum        string             `json:"orderRefNum" binding:"required"`
	State              string             `json:"state" binding:"required"`
	ShipmentDate       *string            `json:"shipmentDate"`
	EstimatedArrival   *string            `json:"estimatedArrival"`
	DeliveredDate      *string            `json:"deliveredDate"`
	Address            Address            `json:"address" binding:"required"`
	ShipmentLineItems  []ShipmentLineItem `json:"lineItems" binding:"required"`
	TrackingNumber     *string            `json:"trackingNumber"`
	ShippingPrice      int                `json:"shippingPrice"`
	Scopable
}
