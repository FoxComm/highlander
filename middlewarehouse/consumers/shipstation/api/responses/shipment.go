package responses

type Shipment struct {
	OrderID        int    `json:"orderId"`
	OrderNumber    string `json:"orderNumber"`
	ShipDate       string `json:"shipDate"`
	TrackingNumber string `json:"trackingNumber"`
}
