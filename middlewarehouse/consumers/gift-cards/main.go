package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
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

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	capConf, err := shared.MakeCaptureConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	client := lib.NewPhoenixClient(capConf.PhoenixURL, capConf.PhoenixUser, capConf.PhoenixPassword)

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	oh, err := NewGiftCardConsumer(client)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
