package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

const (
	clientID = "gift-cards"
	groupID  = "mwh-gift-cards-consumers"
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

	client := phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	gc, err := NewGiftCardConsumer(client)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, gc.Handler)
}
