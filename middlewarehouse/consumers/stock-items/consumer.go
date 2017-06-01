package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
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

func NewStockItemsConsumer(phoenixClient phoenix.PhoenixClient, mwhURL string) (*StockItemsConsumer, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}

	return &StockItemsConsumer{phoenixClient, mwhURL}, nil
}

func (consumer *StockItemsConsumer) sendRequest(url string, body io.Reader, jwt string) {
	completeUrl := fmt.Sprintf("%s/%s", consumer.mwhURL, url)
	log.Println("Sending POST request to: ", completeUrl)
	req, err := http.NewRequest("POST", completeUrl, body)
	if err != nil {
		log.Panicf("Error creating POST request to MWH with error: %s", err.Error())
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("JWT", jwt)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error creating stock_item with error: %s", err.Error())
	}

	if resp.StatusCode == http.StatusOK || resp.StatusCode == http.StatusCreated {
		log.Println("Request processed successfuly")
	} else {
		log.Println("Status code", resp.StatusCode)
		defer resp.Body.Close()
		body, _ := ioutil.ReadAll(resp.Body)
		body_str := string(body)
		log.Println(body_str)
	}
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

	skuReq := sku.CreateSKU()
	skuJson, err := json.Marshal(&skuReq)
	if err != nil {
		log.Panicf("Error marshaling to SKU with error: %s", err.Error())
	}

	log.Printf("Generate Phoenix JWT")
	if err := consumer.phoenixClient.EnsureAuthentication(); err != nil {
		log.Panicf("Error auth in phoenix with error: %s", err.Error())
	}
	jwt := consumer.phoenixClient.GetJwt()

	log.Printf("Send requests to middlewarehouse")
	consumer.sendRequest("v1/public/skus", bytes.NewBuffer(skuJson), jwt)
	consumer.sendRequest("v1/public/stock-items", bytes.NewBuffer(b), jwt)

	log.Printf("Message handler finished processing")
	return nil
}
