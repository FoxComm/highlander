package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/phoenix"
	"github.com/FoxComm/metamorphosis"
)

type OrderConsumer struct {
	topic  string
	client *api.Client
}

func NewOrderConsumer(topic string, key string, secret string) (*OrderConsumer, error) {
	client, err := api.NewClient(key, secret)
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
		log.Panicf("Unable to decode order from activity with error %s", err.Error())
	}

	if fullOrder.Order.OrderState != "fulfillmentStarted" {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Order data: %s. Add to ShipStation!",
		activity.Data(),
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
