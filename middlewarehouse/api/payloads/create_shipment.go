package payloads

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type CreateShipment struct {
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

func (shipment *CreateShipment) Model() *models.Shipment {
	model := &models.Shipment{
		ShippingMethodCode: shipment.ShippingMethodCode,
		OrderRefNum:        shipment.OrderRefNum,
		State:              models.ShipmentState(shipment.State),
		ShipmentDate:       utils.MakeSqlNullString(shipment.ShipmentDate),
		EstimatedArrival:   utils.MakeSqlNullString(shipment.EstimatedArrival),
		DeliveredDate:      utils.MakeSqlNullString(shipment.DeliveredDate),
		AddressID:          shipment.Address.ID,
		Address:            *(shipment.Address.Model()),
		TrackingNumber:     utils.MakeSqlNullString(shipment.TrackingNumber),
		ShippingPrice:      shipment.ShippingPrice,
		Scope:              shipment.Scope,
	}

	for _, lineItem := range shipment.ShipmentLineItems {
		model.ShipmentLineItems = append(model.ShipmentLineItems, *(lineItem.Model()))
	}

	return model
}
