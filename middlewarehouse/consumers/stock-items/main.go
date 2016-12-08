package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	zookeeper := config.ZookeeperURL
	schemaRepo := config.SchemaRepositoryURL
	middlewarehouseURL := config.MiddlewarehouseURL

	capConf, err := shared.MakeCaptureConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	client := phoenix.NewPhoenixClient(capConf.PhoenixURL, capConf.PhoenixUser, capConf.PhoenixPassword)

	consumer, err := NewConsumer(client, zookeeper, schemaRepo, middlewarehouseURL)
	if err != nil {
		log.Panicf("Unable to start consumer with err: %s", err)
	}

	consumer.Run(config.Topic, config.Partition)
}
