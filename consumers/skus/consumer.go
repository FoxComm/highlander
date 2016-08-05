package main

import (
	"bytes"
	"fmt"
	"log"
	"net/http"

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
	sku, err := NewSKUFromAvro(m)
	if err != nil {
		return err
	}

	url := "http://localhost:9292/stock-items"
	jsonStr := []byte(`{"sku": "TEST-FROM-CONSUMER", "stock_location_id": 1, "default_unit_cost": 999}`)

	fmt.Printf("%v\n", sku)

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonStr))
	if err != nil {
		return err
	}

	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Fatalf("Error creating stock_item with error: %s", err.Error())
		return nil
	}

	defer resp.Body.Close()

	fmt.Println("response status:", resp.Status)

	return nil
}
