package consumers

import (
	"log"
	"os"

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
	activity, err := phoenix.NewActivityFromAvro(message)
	if err != nil {
		log.Panicf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type != "order_state_changed" {
		return nil
	}

	fullOrder, err := phoenix.NewFullOrderFromActivity(activity)
	if err != nil {
		log.Panicf("Unable to decode order from activity")
	}

	if fullOrder.Order.OrderState != "fulfillmentStarted" {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Add to ShipStation!",
		fullOrder.Order.ReferenceNumber,
	)

	ssOrder, err := payloads.NewOrderFromPhoenix(fullOrder.Order)
	if err != nil {
		log.Panicf("Unable to create ShipStation order with error %s", err.Error())
	}

	_, err = c.client.CreateOrder(ssOrder)
	if err != nil {
		log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
	}

	return nil
}
