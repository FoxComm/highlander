package main

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
	"github.com/FoxComm/metamorphosis"
	"errors"
)

func main() {
	config, exception := utils.MakeConfig()

	if exception != nil {
		log.Fatalf("Unable to initialize ShipStation with error: %s", exception.ToString())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRegistryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, exception := NewOrderConsumer(config.Topic, config.ApiKey, config.ApiSecret)
	if exception != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", exception.ToString())
	}

	pollingAgent, exception := NewPollingAgent(config.ApiKey, config.ApiSecret, config.MiddlewarehouseURL)
	if exception != nil {
		log.Fatalf("Unable to initialize ShipStation API client with error %s", exception.ToString())
	}

	ticker := time.NewTicker(config.PollingInterval)
	go func() {
		for {
			select {
			case <-ticker.C:
				log.Printf("Querying for new shipments")
				exception = pollingAgent.GetShipments()
				if exception != nil {
					panic(errors.New(exception.ToString()))
				}
			}
		}
	}()

	consumer.RunTopic(config.Topic, config.Partition, oc.Handler)
}
