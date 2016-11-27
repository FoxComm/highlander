package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	clientID = "shipments-01"
	groupID  = "mwh-shipments-consumers"
)

func main() {
	config, exception := consumers.MakeConsumerConfig()
	if exception != nil {
		log.Fatalf("Unable to initialize consumer with error %s", exception.ToString())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	oh, exception := NewOrderHandler(config.MiddlewarehouseURL)
	if exception != nil {
		log.Fatalf("Can't create handler for orders with error %s", exception.ToString())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
