package main

import (
	"fmt"
	"log"
	"os"

	"github.com/FoxComm/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/middlewarehouse/consumers/shipstation/utils"
)

const mwhShipmentsURI = "/v1/public/shipments"

type PollingAgent struct {
	client *api.Client
}

type S struct {
	ReferenceNumber string `json:"referenceNumber"`
	State           string `json:"state"`
	ShipmentDate    string `json:"shipmentDate"`
	TrackingNumber  string `json:"trackingNumber"`
}

func NewPollingAgent() (*PollingAgent, error) {
	key := os.Getenv("API_KEY")
	secret := os.Getenv("API_SECRET")

	client, err := api.NewClient(key, secret)
	if err != nil {
		return nil, err
	}

	return &PollingAgent{client}, nil
}

func (c PollingAgent) GetShipments() error {
	shipments, err := c.client.Shipments()
	if err != nil {
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

		url := fmt.Sprintf("%s/%s/%s", utils.Config.MiddlewarehouseURL, mwhShipmentsURI, s.ReferenceNumber)
		resp := new(S)

		err := httpClient.Patch(url, s, resp)
		if err != nil {
			log.Printf("Failed with error: %s", err.Error())
		}
	}

	return nil
}
