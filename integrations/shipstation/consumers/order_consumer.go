package consumers

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/phoenix"
	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation"
	"github.com/FoxComm/highlander/integrations/shipstation/utils"
	"github.com/FoxComm/metamorphosis"
)

type OrderConsumer struct {
	topic  string
	client *shipstation.Client
}

func NewOrderConsumer(topic string) (*OrderConsumer, error) {
	key := os.Getenv("API_KEY")
	secret := os.Getenv("API_SECRET")

	client, err := shipstation.NewClient(key, secret)
	if err != nil {
		return nil, err
	}

	return &OrderConsumer{topic, client}, nil
}

func (c OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	log.Printf("Received a new message from %s", c.topic)

	order, err := phoenix.NewOrderFromAvro(message)
	if err != nil {
		log.Panicf("Unable to decode Avro message with error %s", err.Error())
	}

	if order.State == "fulfillmentStarted" {
		log.Printf("Handling order with reference number %s", order.ReferenceNumber)

		ssOrder, err := utils.ToShipStationOrder(order)
		if err != nil {
			log.Panicf("Unable to create ShipStation order with error %s", err.Error())
		}

		_, err = c.client.CreateOrder(ssOrder)
		if err != nil {
			log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
		}
	}

	return nil
}

func createShipment() error {
	return nil
}
