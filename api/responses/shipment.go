package responses

import "github.com/FoxComm/middlewarehouse/models"

type Shipment struct {
	ID               uint               `json:"id"`
	ShippingMethodID uint               `json:"shippingMethodId"`
	ReferenceNumber  string             `json:"referenceNumber"`
	State            string             `json:"state"`
	LineItems        []ShipmentLineItem `json:"lineItems"`
	//Transactions     TransactionList    `json:"transactions"`
	ShipmentDate     string             `json:"shipmentDate"`
	EstimatedArrival string             `json:"estimatedArrival"`
	DeliveredDate    string             `json:"deliveredDate"`
	Address          Address            `json:"address"`
	TrackingNumber   string             `json:"trackingNumber"`
}

func NewShipmentFromModel(model *models.Shipment) *Shipment {

	shipment := &Shipment{
		ID:               model.ID,
		ShippingMethodID: model.ShippingMethodID,
		ReferenceNumber:  model.ReferenceNumber,
		State:            model.State,
	}

	if model.ShipmentDate.Valid {
		shipment.ShipmentDate = model.ShipmentDate.String
	}

	if model.EstimatedArrival.Valid {
		shipment.EstimatedArrival = model.EstimatedArrival.String
	}

	if model.DeliveredDate.Valid {
		shipment.DeliveredDate = model.DeliveredDate.String
	}

	return shipment
}
