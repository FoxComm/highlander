package main

import (
	"log"
	"os"
	"strconv"

	"github.com/FoxComm/metamorphosis"
)

const (
	clientID = "capture-01"
	groupID  = "mwh-capture-consumers"
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER")
	schemaRegistry := os.Getenv("SCHEMA_REGISTRY")

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRegistry)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	mwhURL := os.Getenv("MWH_URL")
	oh, err := NewShipmentHandler(mwhURL)
	if err != nil {
		log.Fatalf("Can't create handler for orders with error %s", err.Error())
	}

	topic := os.Getenv("TOPIC")
	partition := os.Getenv("PARTITION")
	partNum, err := strconv.Atoi(partition)
	if err != nil {
		log.Fatalf("Unable to get Kafka partition with error %s", err.Error())
	}

	consumer.RunTopic(topic, partNum, oh.Handler)
}
