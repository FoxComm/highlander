package main

import (
    "log"
    "os"

    "github.com/FoxComm/highlander/giftcard-consumer/consumers"
    "github.com/FoxComm/metamorphosis"
)

const (
    topic     = "activities"
    partition = 1
)



func main() {
    config, err := consumers.MakeConsumerConfig()
    if err != nil {
        log.Fatalf("Unable to initialize consumer with error %s", err.Error())
    }

    consumer, err := metamorphosis.NewConsumer(config.ZookeeperURL, config.SchemaRepositoryURL)
    if err != nil {
        log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
    }

    consumer.SetGroupID(groupID)
    consumer.SetClientID(clientID)

    oh, err := NewGiftCardConsumer(config.MiddlewarehouseURL)
    if err != nil {
        log.Fatalf("Can't create handler for orders with error %s", err.Error())
    }

    consumer.RunTopic(config.Topic, config.Partition, oh.Handler)
}
