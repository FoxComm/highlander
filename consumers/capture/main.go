package main

import (
	"log"
	"os"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/consumers"

	_ "github.com/jpfuentes2/go-env/autoload"
)

const (
	clientID = "capture-01"
	groupID  = "mwh-capture-consumers"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	phoenixURL := os.Getenv("PHOENIX_URL")
	if phoenixURL == "" {
		log.Fatalf("Unable to initialize consumer with error: PHOENIX_URL not found in env")
	}

	phoenixJWT := os.Getenv("PHOENIX_JWT")
	if phoenixJWT == "" {
		log.Fatalf("Unable to initialize consumer with error: PHOENIX_JWT not found in env")
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	oh, err := NewShipmentHandler(config.MiddlewarehouseURL, phoenixURL, phoenixJWT)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
