package main

import (
	"log"
	"os"
	"time"

	"github.com/FoxComm/highlander/integrations/shipstation/consumers"
	"github.com/FoxComm/metamorphosis"
)

const (
	topic     = "activities"
	partition = 1
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER")
	schemaRepo := os.Getenv("SCHEMA_REGISTRY")

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, err := consumers.NewOrderConsumer(topic)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", err.Error())
	}

	shipmentConsumer, err := consumers.NewShipmentConsumer()
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation API client with error %s", err.Error())
	}

	ticker := time.NewTicker(5 * time.Second)
	go func() {
		for {
			select {
			case <-ticker.C:
				log.Printf("Querying for new shipments")
				err = shipmentConsumer.GetShipments()
				if err != nil {
					panic(err)
				}
			}
		}
	}()

	consumer.RunTopic(topic, partition, oc.Handler)
}
