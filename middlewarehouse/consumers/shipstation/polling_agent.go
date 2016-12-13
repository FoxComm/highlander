package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
)

const mwhShipmentsURI = "v1/public/shipments/for-order"

type PollingAgent struct {
	phoenixClient      phoenix.PhoenixClient
	ssClient           *api.Client
	middleWarehouseUrl string
}

type S struct {
	ReferenceNumber string `json:"referenceNumber"`
	State           string `json:"state"`
	ShipmentDate    string `json:"shipmentDate"`
	TrackingNumber  string `json:"trackingNumber"`
}

func NewPollingAgent(phoenixClient phoenix.PhoenixClient, ssClient *api.Client, middleWarehouseUrl string) (*PollingAgent, error) {
	return &PollingAgent{phoenixClient, ssClient, middleWarehouseUrl}, nil
}

func (c PollingAgent) GetShipments() error {
	shipments, err := c.ssClient.Shipments()
	if err != nil {
		log.Printf("Error Getting Shipments: %v", err)
		return err
	}

	if err := c.phoenixClient.EnsureAuthentication(); err != nil {
		log.Panicf("Error auth in phoenix with error: %s", err.Error())
	}

	httpClient := utils.NewHTTPClient()
	httpClient.SetHeader("Content-Type", "application/json")
	httpClient.SetHeader("JWT", c.phoenixClient.GetJwt())

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
