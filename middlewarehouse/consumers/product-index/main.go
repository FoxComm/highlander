package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	zookeeper := config.ZookeeperURL
	schemaRepo := config.SchemaRepositoryURL

	consumer, err := NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		log.Fatalf("Unable to start consumer with err: %s", err)
	}

	consumer.Run(config.Topic, config.Partition)
}
