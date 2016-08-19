package responses

type Shipments struct {
	Shipments      []Shipment `json:"shipments"`
	UnshippedItems []ShipmentLineItem`json:"unshippedItems"`
}
