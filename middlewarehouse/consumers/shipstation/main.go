package main

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
	"github.com/FoxComm/metamorphosis"
)

const (
	topic     = "activities"
	partition = 1
)

func main() {
	if err := utils.InitializeConfig(); err != nil {
		log.Fatalf("Unable to initialize ShipStation with error: %s", err.Error())
	}

	zookeeper := utils.Config.ZookeeperURL
	schemaRegistry := utils.Config.SchemaRegistryURL

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRegistry)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, err := NewOrderConsumer(topic)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", err.Error())
	}

	pollingAgent, err := NewPollingAgent()
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation API client with error %s", err.Error())
	}

	ticker := time.NewTicker(5 * time.Second)
	go func() {
		for {
			select {
			case <-ticker.C:
				log.Printf("Querying for new shipments")
				err = pollingAgent.GetShipments()
				if err != nil {
					panic(err)
				}
			}
		}
	}()

	consumer.RunTopic(topic, partition, oc.Handler)
}
