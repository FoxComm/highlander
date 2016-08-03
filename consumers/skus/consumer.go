package main

import (
	"fmt"

	"github.com/FoxComm/metamorphosis"
)

const topic = "sku_search_view"
const partition = 1

type Consumer struct {
	c metamorphosis.Consumer
}

func NewConsumer(zookeeper string, schemaRepo string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		return nil, err
	}

	return &Consumer{c: consumer}, nil
}

func (consumer *Consumer) Run() {
	consumer.c.RunTopic(topic, partition, consumer.handler)
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	fmt.Println(string(m.Bytes()))
	return nil
}
