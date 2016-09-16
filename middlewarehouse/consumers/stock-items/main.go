package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	_ "github.com/jpfuentes2/go-env/autoload"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	zookeeper := config.ZookeeperURL
	schemaRepo := config.SchemaRepositoryURL
	middlewarehouseURL := config.MiddlewarehouseURL

	consumer, err := NewConsumer(zookeeper, schemaRepo, middlewarehouseURL)
	if err != nil {
		log.Panicf("Unable to start consumer with err: %s", err)
	}

	consumer.Run(config.Topic, config.Partition)
}
