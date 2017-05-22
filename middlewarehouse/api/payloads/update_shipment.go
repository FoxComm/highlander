package payloads

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/db/utils"
	"github.com/FoxComm/highlander/middlewarehouse/models"
)

type UpdateShipment struct {
	ShippingMethodCode string   `json:"shippingMethodCode"`
	State              string   `json:"state""`
	ShipmentDate       *string  `json:"shipmentDate"`
	EstimatedArrival   *string  `json:"estimatedArrival"`
	DeliveredDate      *string  `json:"deliveredDate"`
	Address            *Address `json:"address"`
	TrackingNumber     *string  `json:"trackingNumber"`
	ShippingPrice      *int     `json:"shippingPrice"`
	Scopable
}

func (payload *UpdateShipment) Model() *models.Shipment {
	shipment := new(models.Shipment)

	if payload.ShippingMethodCode != "" {
		shipment.ShippingMethodCode = payload.ShippingMethodCode
	}

	if payload.State != "" {
		shipment.State = models.ShipmentState(payload.State)
	}

	if payload.ShippingPrice != nil {
		shipment.ShippingPrice = *(payload.ShippingPrice)
	}

	shipment.ShipmentDate = utils.MakeSqlNullString(payload.ShipmentDate)
	shipment.EstimatedArrival = utils.MakeSqlNullString(payload.EstimatedArrival)
	shipment.DeliveredDate = utils.MakeSqlNullString(payload.DeliveredDate)
	shipment.TrackingNumber = utils.MakeSqlNullString(payload.TrackingNumber)

	if payload.Address != nil {
		shipment.AddressID = payload.Address.ID
		shipment.Address = *(payload.Address.Model())
	}

	shipment.Scope = payload.Scope
	return shipment
}
