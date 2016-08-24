package consumers

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation"
)

type ShipmentConsumer struct {
	client *shipstation.Client
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

	log.Printf("%s", shipments.Shipments)
	return nil
}
