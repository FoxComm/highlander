package main

import (
	"log"
	"os"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/consumers"
	"github.com/FoxComm/middlewarehouse/consumers/capture/lib"

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

	phoenixUser := os.Getenv("PHOENIX_USER")
	if phoenixUser == "" {
		log.Fatalf("Unable to initialize consumer with error: PHOENIX_USER not found in env")
	}

	phoenixPassword := os.Getenv("PHOENIX_PASSWORD")
	if phoenixPassword == "" {
		log.Fatalf("Unable to initialize consumer with error: PHOENIX_PASSWORD not found in env")
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	client := lib.NewPhoenixClient(phoenixURL, phoenixUser, phoenixPassword)
	if err := client.Authenticate(); err != nil {
		log.Fatalf("Unable to authenticate with Phoenix")
	}

	oh, err := NewShipmentHandler(config.MiddlewarehouseURL, client)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
