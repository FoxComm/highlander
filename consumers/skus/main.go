package main

import "log"

func main() {
	zookeeper := "localhost:2181"
	schemaRepo := "http://localhost:8081"

	consumer, err := NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		log.Panicf("Unable to start consumer with err: %s", err)
	}

	consumer.Run()
}
