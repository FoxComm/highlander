package payloads

type Shipment struct {
	ShippingMethodID uint   `json:"shippingMethodId" binding:"required"`
	ReferenceNumber  string `json:"referenceNumber" binding:"required"`
	State            string `json:"state" binding:"required"`
	ShipmentDate     string `json:"shipmentDate"`
	EstimatedArrival string `json:"estimatedArrival"`
	DeliveredDate    string `json:"deliveredDate"`
}

type ShipmentFull struct {
	Shipment
	LineItems []ShipmentLineItem `json:"lineItems" binding:"required"`
	Address   Address            `json:"address" binding:"required"`
}

func NewShipmentFromShipmentFull(payloadFull *ShipmentFull) *Shipment {
	return &Shipment{
		payloadFull.ShippingMethodID,
		payloadFull.ReferenceNumber,
		payloadFull.State,
		payloadFull.ShipmentDate,
		payloadFull.EstimatedArrival,
		payloadFull.DeliveredDate,
	}
}