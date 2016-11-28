package main

import (
	"fmt"
	"log"

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

func NewPollingAgent(key string, secret string, middleWarehouseUrl string) (*PollingAgent, error) {

	client, err := api.NewClient(key, secret)
	if err != nil {
		return nil, err
	}

	return &PollingAgent{client, middleWarehouseUrl}, nil
}

func (c PollingAgent) GetShipments() error {
	shipments, err := c.client.Shipments()
	if err != nil {
		log.Printf("Error Getting Shipments: %v", err)
		return err
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

		err := httpClient.Patch(url, s, resp)
		if err != nil {
			log.Printf("Failed %s with error: %s", url, err.Error())
		}
	}

	return nil
}
