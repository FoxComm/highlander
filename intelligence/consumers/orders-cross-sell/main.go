package main

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	groupId  = "orders-2"
	clientId = "hal-orders-cross-sell"
)

var apiUrl = os.Getenv("API_URL")

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL, metamorphosis.OffsetResetLargest)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupId)
	consumer.SetClientID(clientId)

	oc, err := NewOrderConsumer(apiUrl)

	consumer.RunTopic(config.Topic, oc.Handler)
}
