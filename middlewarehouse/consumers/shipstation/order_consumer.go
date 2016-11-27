package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/phoenix"
	"github.com/FoxComm/metamorphosis"
)

type OrderConsumer struct {
	topic  string
	client *api.Client
}

func NewOrderConsumer(topic string, key string, secret string) (*OrderConsumer, exceptions.IException) {
	client, exception := api.NewClient(key, secret)
	if exception != nil {
		return nil, exception
	}

	return &OrderConsumer{topic, client}, nil
}

func (c OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, exception := phoenix.NewActivityFromAvro(message)
	if exception != nil {
		log.Panicf("Unable to decode Avro message with error %s", exception.ToString())
	}

	if activity.Type != "order_state_changed" {
		return nil
	}

	fullOrder, exception := phoenix.NewFullOrderFromActivity(activity)
	if exception != nil {
		log.Panicf("Unable to decode order from activity")
	}

	if fullOrder.Order.OrderState != "fulfillmentStarted" {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Add to ShipStation!",
		fullOrder.Order.ReferenceNumber,
	)

	ssOrder, exception := payloads.NewOrderFromPhoenix(fullOrder.Order)
	if exception != nil {
		log.Panicf("Unable to create ShipStation order with error %s", exception.ToString())
	}

	_, exception = c.client.CreateOrder(ssOrder)
	if exception != nil {
		log.Panicf("Unable to create order in ShipStation with error %s", exception.ToString())
	}

	return nil
}
