package main

import (
	"log"

	"github.com/FoxComm/highlander/integrations/shipstation/consumers"
	"github.com/FoxComm/metamorphosis"
)

// This listens to  "orders_search_view"
func main() {
    config, err := consumers.MakeConsumerConfig()

    if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRegistryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, err := consumers.NewOrderConsumer(config.Topic, config.ApiKey, config.ApiSecret)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, config.Partition, oc.Handler)
}
