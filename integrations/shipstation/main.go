package main

import (
	"github.com/FoxComm/shipstation/kafka"
)

func main() {
	zookeeper := "localhost:2181"
	schemaRepo := "http://localhost:8081"

	consumer, err := kafka.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		panic(err)
	}

	consumer.RunTopic("orders_search_view")
}
