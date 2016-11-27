package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/metamorphosis"

	_ "github.com/jpfuentes2/go-env/autoload"
)

const (
	clientID = "capture-01"
	groupID  = "mwh-capture-consumers"
)

func main() {
	config, exception := consumers.MakeConsumerConfig()
	if exception != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", exception.ToString())
	}

	capConf, exception := shared.MakeCaptureConsumerConfig()
	if exception != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", exception.ToString())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	client := lib.NewPhoenixClient(capConf.PhoenixURL, capConf.PhoenixUser, capConf.PhoenixPassword)
	if exception := client.Authenticate(); exception != nil {
		log.Fatalf("Unable to authenticate with Phoenix with error %s", exception.ToString())
	}

	oh, exception := NewShipmentHandler(config.MiddlewarehouseURL, client)
	if exception != nil {
		log.Fatalf("Can't create handler for orders with error %s", exception.ToString())
	}

	consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
