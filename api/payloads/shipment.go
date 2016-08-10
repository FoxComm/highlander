package payloads

type Shipment struct {
	ShippingMethodID uint               `json:"shippingMethodId" binding:"required"`
	ReferenceNumber  string             `json:"referenceNumber" binding:"required"`
	State            string             `json:"state" binding:"required"`
	ShipmentDate     *string            `json:"shipmentDate"`
	EstimatedArrival *string            `json:"estimatedArrival"`
	DeliveredDate    *string            `json:"deliveredDate"`
	Address          Address            `json:"address" binding:"required"`
	LineItems        []ShipmentLineItem `json:"lineItems" binding:"required"`
	TrackingNumber   *string            `json:"trackingNumber"`
}
