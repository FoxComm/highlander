package main

import (
	"log"
	"os"

	"github.com/FoxComm/shipstation/lib/kafka"
	"github.com/FoxComm/shipstation/lib/phoenix"
	"github.com/FoxComm/shipstation/lib/shipstation"
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER")
	schemaRepo := os.Getenv("SCHEMA_REGISTRY")

	consumer, err := kafka.NewConsumer(zookeeper, schemaRepo, defaultMessageHandler)
	if err != nil {
		panic(err)
	}

	consumer.RunTopic("orders_search_view")
}

func defaultMessageHandler(message *[]byte) error {
	log.Println("Received a new message from orders_search_view")

	order, err := phoenix.NewOrderFromAvro(*message)
	if err != nil {
		log.Printf("Unable to decode Avro message with error %s", err.Error())
		return nil
	}

	if order.State == "fulfillmentStarted" {
		ssOrder, err := toShipStationOrder(order)
		if err != nil {
			log.Printf("Unable to create ShipStation order with error %s", err.Error())
			return nil
		}

		key := os.Getenv("API_KEY")
		secret := os.Getenv("API_SECRET")

		client, err := shipstation.NewClient(key, secret)
		if err != nil {
			log.Panicf("Unable to create ShipStation client with error %s", err.Error())
		}

		_, err = client.CreateOrder(ssOrder)
		if err != nil {
			log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
		}
	}

	return nil
}
