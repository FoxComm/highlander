package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/metamorphosis"
)

func main() {
	zookeeper := "localhost:2181"
	schemaRepo := "http://localhost:8081"

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		log.Panicf("Unable to start consumer with err: %s", err)
	}

	handler := func(message metamorphosis.AvroMessage) error {
		fmt.Println(string(message.Bytes()))
		return nil
	}

	consumer.RunTopic("sku_search_view", 1, handler)
}
