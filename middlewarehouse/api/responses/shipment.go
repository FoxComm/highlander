package responses

import (
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type Shipment struct {
	ID                uint               `json:"id"`
	ReferenceNumber   string             `json:"referenceNumber"`
	ShippingMethod    ShippingMethod     `json:"shippingMethod"`
	OrderRefNum       string             `json:"orderRefNum"`
	State             string             `json:"state"`
	ShipmentDate      *string            `json:"shipmentDate"`
	EstimatedArrival  *string            `json:"estimatedArrival"`
	DeliveredDate     *string            `json:"deliveredDate"`
	Address           Address            `json:"address"`
	ShipmentLineItems []ShipmentLineItem `json:"lineItems"`
	TrackingNumber    *string            `json:"trackingNumber"`
	ShippingPrice     int                `json:"shippingPrice"`
}

func NewShipmentFromModel(model *models.Shipment) (*Shipment, exceptions.IException) {
	shippingMethod, exception := NewShippingMethodFromModel(&model.ShippingMethod)
	if exception != nil {
		return nil, exception
	}

	shipment := &Shipment{
		ID:                model.ID,
		ReferenceNumber:   model.ReferenceNumber,
		ShippingMethod:    *shippingMethod,
		OrderRefNum:       model.OrderRefNum,
		State:             string(model.State),
		ShipmentDate:      NewStringFromSqlNullString(model.ShipmentDate),
		EstimatedArrival:  NewStringFromSqlNullString(model.EstimatedArrival),
		DeliveredDate:     NewStringFromSqlNullString(model.DeliveredDate),
		Address:           *NewAddressFromModel(&model.Address),
		ShipmentLineItems: make([]ShipmentLineItem, 0),
		TrackingNumber:    NewStringFromSqlNullString(model.TrackingNumber),
		ShippingPrice:     model.ShippingPrice,
	}

	for i := range model.ShipmentLineItems {
		lineItem := NewShipmentLineItemFromModel(&model.ShipmentLineItems[i])

		shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *lineItem)
	}

	return shipment, nil
}
