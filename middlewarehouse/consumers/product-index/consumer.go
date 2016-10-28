package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/shared/golang/api"
	"github.com/FoxComm/metamorphosis"
)

type Consumer struct {
	c metamorphosis.Consumer
}

const (
	clientID = "product-indexer-01"
	groupID  = "product-indexer-group"
)

func NewConsumer(zookeeper, schemaRepo string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		return nil, err
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	return &Consumer{c: consumer}, nil
}

func (consumer *Consumer) Run(topic string, partition int) {
	consumer.c.RunTopic(topic, partition, consumer.handler)
}

type ConsumerProduct struct {
	Admin   interface{}  `json:"admin"`
	Product *api.Product `json:"product"`
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	log.Printf("Received message %s", string(m.Bytes()))

	activity, err := activities.NewActivityFromAvro(m)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type() != "full_product_created" && activity.Type() != "full_product_updated" {
		return nil
	}

	log.Printf("We got one!")
	log.Printf("%v", activity.Data())

	bt := []byte(activity.Data())
	prod := new(ConsumerProduct)

	if err := json.Unmarshal(bt, prod); err != nil {
		return fmt.Errorf("Error unmarshalling activity data into product with error: %s", err.Error())
	}

	visualVariants := []string{"Color", "Fabric"}
	partialProducts, err := MakePartialProducts(prod.Product, visualVariants)
	if err != nil {
		return fmt.Errorf("Error creating partial products with error: %s", err.Error())
	}

	for _, p := range partialProducts {
		log.Printf("Partial product: %v", p)
	}

	return nil
}
