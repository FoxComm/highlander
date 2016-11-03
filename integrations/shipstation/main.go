package main

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/integrations/shipstation/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	topic     = "orders_search_view"
	partition = 1
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER")
	schemaRepo := os.Getenv("SCHEMA_REGISTRY")

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, err := consumers.NewOrderConsumer(topic)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", err.Error())
	}

	consumer.RunTopic(topic, partition, oc.Handler)
}
