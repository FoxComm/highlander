package main

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	clientId = "orders-01"
	groupId  = "hal-orders-consumers"
)

var henhouseHost = os.Getenv("HENHOUSE")

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

	oc, err := NewOrderConsumer(henhouseHost)

	consumer.RunTopic(config.Topic, oc.Handler)
}
