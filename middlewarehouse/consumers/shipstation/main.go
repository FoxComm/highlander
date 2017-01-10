package main

import (
	"log"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	phoenixConfig, err := shared.MakePhoenixConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	ssConfig, err := utils.MakeConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error: %s", err.Error())
	}

	phoenixClient := phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)
	shipStationClient, err := api.NewClient(ssConfig.ApiKey, ssConfig.ApiSecret)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation client with error: %s", err.Error())
	}

	pollingAgent, err := NewPollingAgent(phoenixClient, shipStationClient, config.MiddlewarehouseURL)
	if err != nil {
		log.Fatalf("Unable to initialize ShipStation API client with error %s", err.Error())
	}

	consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL, config.OffsetResetStrategy)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	ticker := time.NewTicker(ssConfig.PollingInterval)

	go func() {
		for {
			select {
			case <-ticker.C:
				if err := pollingAgent.GetShipments(); err != nil {
					panic(err)
				}
			}
		}
	}()

	oh := NewOrderConsumer(phoenixClient, shipStationClient)

	consumer.RunTopic(config.Topic, oh.Handler)
}
