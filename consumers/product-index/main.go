package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
)

func main() {
	config, err := consumers.MakeConsumerConfig()
	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}
	idxConfig, err := MakeIndexerConfig()

	if err != nil {
		log.Fatalf("Unable to initialize consumer with error %s", err.Error())
	}

	log.Printf("visualVariants: %v\n", idxConfig.VisualVariants)

	consumer, err := NewConsumer(&config, &idxConfig)
	if err != nil {
		log.Fatalf("Unable to start consumer with err: %s", err)
	}

	consumer.Run(config.Topic)
}
