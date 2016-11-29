package main

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
	"github.com/FoxComm/metamorphosis"
)

func main() {
	config, err := utils.MakeConfig()

	if err != nil {
		log.Fatalf("Unable to initialize ShipStation with error: %s", err.Error())
	}

	client := lib.NewPhoenixClient(config.PhoenixURL, config.PhoenixUser, config.PhoenixPassword)
	if err := client.Authenticate(); err != nil {
		log.Fatalf("Unable to authenticate with Phoenix with error %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRegistryURL)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	oc, err := NewOrderConsumer(client, config.Topic, config.ApiKey, config.ApiSecret)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation order consumer with error %s", err.Error())
	}

	pollingAgent, err := NewPollingAgent(config.ApiKey, config.ApiSecret, config.MiddlewarehouseURL)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation API client with error %s", err.Error())
	}

	ticker := time.NewTicker(config.PollingInterval)
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

	consumer.RunTopic(config.Topic, config.Partition, oc.Handler)
}
