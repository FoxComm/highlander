package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
)

const mwhShipmentsURI = "v1/public/shipments/for-order"

type PollingAgent struct {
	client             *api.Client
	middleWarehouseUrl string
}

type S struct {
	ReferenceNumber string `json:"referenceNumber"`
	State           string `json:"state"`
	ShipmentDate    string `json:"shipmentDate"`
	TrackingNumber  string `json:"trackingNumber"`
}

func NewPollingAgent(key string, secret string, middleWarehouseUrl string) (*PollingAgent, exceptions.IException) {

	client, exception := api.NewClient(key, secret)
	if exception != nil {
		return nil, exception
	}

	return &PollingAgent{client, middleWarehouseUrl}, nil
}

func (c PollingAgent) GetShipments() exceptions.IException {
	shipments, exception := c.client.Shipments()
	if exception != nil {
		log.Printf("Error Getting Shipments: %v", exception)
		return exception
	}

	httpClient := utils.NewHTTPClient()
	httpClient.SetHeader("Content-Type", "application/json")
	for _, shipment := range shipments.Shipments {
		log.Printf("Processing shipment %s", shipment.OrderNumber)

		s := S{
			ReferenceNumber: shipment.OrderNumber,
			State:           "shipped",
			ShipmentDate:    shipment.ShipDate,
			TrackingNumber:  shipment.TrackingNumber,
		}

		url := fmt.Sprintf("%s/%s/%s", c.middleWarehouseUrl, mwhShipmentsURI, s.ReferenceNumber)
		resp := new(S)

		exception := httpClient.Patch(url, s, resp)
		if exception != nil {
			log.Printf("Failed %s with error: %s", url, exception.ToString())
		}
	}

	return nil
}
