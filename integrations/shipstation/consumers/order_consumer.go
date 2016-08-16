package consumers

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/mwh"
	"github.com/FoxComm/highlander/integrations/shipstation/lib/phoenix"
	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation"
	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation/payloads"
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
	order, err := phoenix.NewOrderFromAvro(message)
	if err != nil {
		log.Panicf("Unable to decode Avro message with error %s", err.Error())
	}

	if order.State == "fulfillmentStarted" {
		orderStr := string(message.Bytes())
		log.Printf("Handling order with reference number %s", order.ReferenceNumber)
		log.Printf(orderStr)

		ssOrder, err := payloads.NewOrderFromPhoenix(order)
		if err != nil {
			log.Panicf("Unable to create ShipStation order with error %s", err.Error())
		}

		_, err = c.client.CreateOrder(ssOrder)
		if err != nil {
			log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
		}

		if err := createShipment(order); err != nil {
			log.Panicf("Unable to create shipment in middlewarehouse with error %s", err.Error())
		}
	}

	return nil
}

func createShipment(o *phoenix.Order) error {
	shipment, err := mwh.NewShipmentFromOrder(o)
	if err != nil {
		return err
	}

	mwhClient := mwh.NewClient()
	_, err = mwhClient.CreateShipment(shipment)
	if err != nil {
		return err
	}

	return nil
}
