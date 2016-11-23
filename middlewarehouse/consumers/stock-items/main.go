package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
)

func main() {
	config, exception := consumers.MakeConsumerConfig()
	if exception != nil {
		log.Fatalf("Unable to initialize consumer with error %s", exception.ToString())
	}

	zookeeper := config.ZookeeperURL
	schemaRepo := config.SchemaRepositoryURL
	middlewarehouseURL := config.MiddlewarehouseURL

	consumer, exception := NewConsumer(zookeeper, schemaRepo, middlewarehouseURL)
	if exception != nil {
		log.Panicf("Unable to start consumer with exception: %s", exception.ToString())
	}

	consumer.Run(config.Topic, config.Partition)
}

type stockItemsConsumerException struct {
	Type string `json:"type"`
	exceptions.Exception
}

func (exception stockItemsConsumerException) ToJSON() interface{} {
	return exception
}

func NewStockItemsConsumerException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return stockItemsConsumerException{
		Type:      "stockItemsConsumer",
		Exception: exceptions.Exception{error.Error()},
	}
}
