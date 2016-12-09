package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

type Consumer struct {
	c             metamorphosis.Consumer
	phoenixClient phoenix.PhoenixClient
	mwhURL        string
}

const (
	clientID = "stock-items-01"
	groupID  = "mwh-stock-items-consumers"
)

func NewConsumer(phoenixClient phoenix.PhoenixClient, zookeeper string, schemaRepo string, mwhURL string) (*Consumer, error) {
	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
	if err != nil {
		return nil, err
	}

	consumer.SetGroupID(groupID)
	consumer.SetClientID(clientID)

	return &Consumer{c: consumer, mwhURL: mwhURL, phoenixClient: phoenixClient}, nil
}

func (consumer *Consumer) Run(topic string, partition int) {
	consumer.c.RunTopic(topic, partition, consumer.handler)
}

func (consumer *Consumer) handler(m metamorphosis.AvroMessage) error {
	log.Printf("Received SKU %s", string(m.Bytes()))

	sku, err := NewSKUFromAvro(m)
	if err != nil {
		log.Panicf("Error unmarshaling from Avro with error: %s", err.Error())
	}

	stockItem := sku.StockItem(1)
	b, err := json.Marshal(&stockItem)
	if err != nil {
		log.Panicf("Error marshaling to stock item with error: %s", err.Error())
	}

	url := fmt.Sprintf("%s/v1/public/stock-items", consumer.mwhURL)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
	if err != nil {
		log.Panicf("Error creating POST request to MWH with error: %s", err.Error())
	}

	if err := consumer.phoenixClient.EnsureAuthentication(); err != nil {
		log.Panicf("Error auth in phoenix with error: %s", err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("JWT", consumer.phoenixClient.GetJwt())

	client := &http.Client{}
	if _, err := client.Do(req); err != nil {
		log.Printf("Error creating stock_item with error: %s", err.Error())
	}

	return nil
}
