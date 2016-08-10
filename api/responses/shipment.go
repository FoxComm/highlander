package responses

import "github.com/FoxComm/middlewarehouse/models"

type Shipment struct {
	ID                uint               `json:"id"`
	ShippingMethod    ShippingMethod     `json:"shippingMethod"`
	ReferenceNumber   string             `json:"referenceNumber"`
	State             string             `json:"state"`
	ShipmentDate      *string            `json:"shipmentDate"`
	EstimatedArrival  *string            `json:"estimatedArrival"`
	DeliveredDate     *string            `json:"deliveredDate"`
	Address           Address            `json:"address"`
	ShipmentLineItems []ShipmentLineItem `json:"lineItems"`
	TrackingNumber    *string            `json:"trackingNumber"`
}

func NewShipmentFromModel(model *models.Shipment) *Shipment {
	shipment := &Shipment{
		ID:                model.ID,
		ShippingMethod:    *NewShippingMethodFromModel(&model.ShippingMethod),
		ReferenceNumber:   model.ReferenceNumber,
		State:             string(model.State),
		ShipmentDate:      NewStringFromSqlNullString(model.ShipmentDate),
		EstimatedArrival:  NewStringFromSqlNullString(model.EstimatedArrival),
		DeliveredDate:     NewStringFromSqlNullString(model.DeliveredDate),
		Address:           *NewAddressFromModel(&model.Address),
		ShipmentLineItems: make([]ShipmentLineItem, 0),
		TrackingNumber:    NewStringFromSqlNullString(model.TrackingNumber),
	}

	for _, lineItem := range model.ShipmentLineItems {
		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *NewShipmentLineItemFromModel(&lineItem))
	}

	return shipment
}
