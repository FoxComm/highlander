package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	phoenixConfig, err := shared.MakePhoenixConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL, config.OffsetResetStrategy)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	phoenixClient := phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)
	si, err := NewStockItemsConsumer(phoenixClient, config.MiddlewarehouseURL)

	if err != nil {
		log.Fatalf("Can't create handler for stock items with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, si.Handler)
}
