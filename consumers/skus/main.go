package main

import (
	"log"
	"os"

	_ "github.com/jpfuentes2/go-env/autoload"
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER_URL")
	schemaRepo := os.Getenv("SCHEMA_REPO_URL")
	middlewarehouseURL := os.Getenv("MWH_URL")
	topic := os.Getenv("TOPIC")

	consumer, err := NewConsumer(zookeeper, schemaRepo, middlewarehouseURL)
	if err != nil {
		log.Panicf("Unable to start consumer with err: %s", err)
	}

	consumer.Run(topic, 1)
}
