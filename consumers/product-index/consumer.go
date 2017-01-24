package main

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/shared/golang/api"
	"github.com/FoxComm/metamorphosis"
)

type Consumer struct {
	c metamorphosis.Consumer
	i *Indexer
}

const (
	clientID = "product-indexer-01"
	groupID  = "product-indexer-group"
)

func NewConsumer(zookeeper, schemaRepo, offsetStrategy string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo, offsetStrategy)
	if err != nil {
		return nil, err
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	visualVariants := []string{"color", "pattern", "material", "style"}
	idxer, err := NewIndexer("http://localhost:9200", "public", "products_catalog_view", visualVariants)
	if err != nil {
		return nil, err
	}

	return &Consumer{c: consumer, i: idxer}, nil
}

func (consumer *Consumer) Run(topic string) {
	consumer.c.RunTopic(topic, consumer.handler)
}

type ConsumerProduct struct {
	Admin   interface{}  `json:"admin"`
	Product *api.Product `json:"product"`
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(m)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	return consumer.i.Run(activity)
}
