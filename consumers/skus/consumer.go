package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/FoxComm/metamorphosis"
)

type Consumer struct {
	c      metamorphosis.Consumer
	mwhURL string
}

func NewConsumer(zookeeper string, schemaRepo string, mwhURL string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		return nil, err
	}

	return &Consumer{c: consumer, mwhURL: mwhURL}, nil
}

func (consumer *Consumer) Run(topic string, partition int) {
	consumer.c.RunTopic(topic, partition, consumer.handler)
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	sku, err := NewSKUFromAvro(m)
	if err != nil {
		return err
	}

	stockItem := sku.StockItem(1)
	b, err := json.Marshal(&stockItem)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/stock-items", consumer.mwhURL)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
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
