package consumers

import (
	"fmt"
	"log"
	"os"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation"
	"github.com/FoxComm/highlander/integrations/shipstation/utils"
)

type ShipmentConsumer struct {
	client *shipstation.Client
}

type S struct {
	ReferenceNumber string `json:"referenceNumber"`
	State           string `json:"state"`
	ShipmentDate    string `json:"shipmentDate"`
	TrackingNumber  string `json:"trackingNumber"`
}

func NewShipmentConsumer() (*ShipmentConsumer, error) {
	key := os.Getenv("API_KEY")
	secret := os.Getenv("API_SECRET")

	client, err := shipstation.NewClient(key, secret)
	if err != nil {
		return nil, err
	}

	return &ShipmentConsumer{client}, nil
}

func (c ShipmentConsumer) GetShipments() error {
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

		url := fmt.Sprintf("http://127.0.0.1:9292/v1/public/shipments/%s", s.ReferenceNumber)
		resp := new(S)

		err := httpClient.Patch(url, s, resp)
		if err != nil {
			log.Printf("Failed with error: %s", err.Error())
		}
	}

	return nil
}
