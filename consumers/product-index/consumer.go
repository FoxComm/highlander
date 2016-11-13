package main

import (
	"github.com/FoxComm/highlander/shared/golang/activities"
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

func NewConsumer(zookeeper, schemaRepo string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
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

func (consumer *Consumer) Run(topic string, partition int) {
	consumer.c.RunTopic(topic, partition, consumer.handler)
}

type ConsumerProduct struct {
	Admin   interface{}  `json:"admin"`
	Product *api.Product `json:"product"`
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	activity, err := activities.CreateActivity(m)
	if err != nil {
		return err
	}

	return consumer.i.Run(activity)
}
