package main

import (
	"encoding/json"
	"errors"
	"log"
	"net"
	"strconv"

	"bytes"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

type Sku struct {
	ProductId int `json:"productFormId"`
}

type LineItems struct {
	Skus []Sku `json:"skus"`
}

type Customer struct {
	Id int `json:"id"`
}

type Order struct {
	Customer  Customer  `json:"customer"`
	LineItems LineItems `json:"lineItems"`
}

type Activity struct {
	Order Order `json:"order"`
}

type urlGetter func() (string, error)
type OrderConsumer struct {
	apiUrl urlGetter
}

type ProdProdPayload struct {
	CustID  int   `json:"cust_id"`
	ProdIDs []int `json:"prod_ids"`
	ChanID  int   `json:"channel_id"`
}

const (
	orderCheckoutCompleted = "order_checkout_completed"
)

func NewOrderConsumer(anthillHost string) (*OrderConsumer, error) {
	if anthillHost == "" {
		return nil, errors.New("anthill host is required")
	}

	anthillUrl := lookupSrv(anthillHost)
	return &OrderConsumer{anthillUrl}, nil
}

func (o OrderConsumer) parseData(data string) error {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		log.Fatalf("Activity Parse Error %s", jsonErr.Error())
		return jsonErr
	}

	skus := act.Order.LineItems.Skus
	payload := ProdProdPayload{
		CustID: act.Order.Customer.Id,
		ChanID: 1}
	for i := 0; i < len(skus); i++ {
		payload.ProdIDs = append(payload.ProdIDs, skus[i].ProductId)
	}

	err := o.track(payload)
	return err
}

func (o OrderConsumer) track(payload ProdProdPayload) error {
	body, jsonErr := json.Marshal(payload)
	if jsonErr != nil {
		log.Fatalf("Payload marshal error: %s", jsonErr.Error())
		return jsonErr
	}
	apiString, err1 := o.apiUrl()
	if err1 != nil {
		log.Fatalf("Api Url Error: %s", err1.Error())
		return err1
	}
	_, err := http.Post(apiString, "application/json", bytes.NewBuffer(body))
	return err
}

func (o OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
		return err
	}

	if activity.Type() == orderCheckoutCompleted {
		err := o.parseData(activity.Data())
		if err != nil {
			log.Fatalf("Unable to parse json with error %s", err.Error())
		}
		return err
	}
	return nil
}

func lookupSrv(host string) func() (string, error) {
	return func() (string, error) {
		_, srvs, err := net.LookupSRV("", "", host)
		if err != nil {
			return "", err
		}

		if len(srvs) == 0 {
			return "", errors.New("Unable to find port for " + host)
		}

		srv := srvs[0]

		port := strconv.Itoa(int(srv.Port))
		anthillUrl := "http://" + host + ":" + port + "/private/prod-prod/train"

		return anthillUrl, nil
	}
}
