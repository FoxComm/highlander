package main

import (
	"encoding/json"
	"errors"
	"log"

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

type OrderConsumer struct {
	apiUrl string
}

type Point struct {
	CustID int `json:"custID"`
	ProdID int `json:"prodID"`
	ChanID int `json:"chanID"`
}

type ProdProdPayload struct {
	Points []Point `json:"points"`
}

const (
	orderCheckoutCompleted = "order_checkout_completed"
)

func NewOrderConsumer(apiUrl string) (*OrderConsumer, error) {
	if apiUrl == "" {
		return nil, errors.New("cross sell host is required")
	}

	crossSellUrl := apiUrl + "/public/recommend/prod-prod/train"
	return &OrderConsumer{crossSellUrl}, nil
}

func (o OrderConsumer) parseData(data string) error {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		return jsonErr
	}

	skus := act.Order.LineItems.Skus
	payload := ProdProdPayload{}
	for i := 0; i < len(skus); i++ {
		payload.Points = append(payload.Points, Point{
			CustID: act.Order.Customer.Id,
			ProdID: skus[i].ProductId,
			ChanID: 1})
	}

	err := o.track(payload)
	return err
}

func (o OrderConsumer) track(payload ProdProdPayload) error {
	body, jsonErr := json.Marshal(payload)
	if jsonErr != nil {
		return jsonErr
	}
	_, err := http.Post(o.apiUrl, "application/json", bytes.NewBuffer(body))
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
