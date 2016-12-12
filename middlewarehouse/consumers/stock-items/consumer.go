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

type StockItemsConsumer struct {
	phoenixClient phoenix.PhoenixClient
	mwhURL        string
}

const (
	clientID = "stock-items-01"
	groupID  = "mwh-stock-items-consumers"
)

func NewStockItemsConsumer(phoenixClient phoenix.PhoenixClient, mwhUrl string) (*StockItemsConsumer, error) {
	return &StockItemsConsumer{phoenixClient, mwhUrl}, nil
}

func (consumer *StockItemsConsumer) Handler(m metamorphosis.AvroMessage) error {
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
