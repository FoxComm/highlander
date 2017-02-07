package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	orderCheckoutCompleted = "cart_line_items_updated_quantities"
	groupId                = "orders-1"
	clientId               = "hal-orders-consumers"
)

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

	consumer.RunTopic(config.Topic, Handler)
}
