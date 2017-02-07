package main

import (
	"encoding/json"
	"fmt"
	"log"

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
	Order Order `json:"cart"`
}

func parseData(data string) error {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		return jsonErr
	}

	skus := act.Order.LineItems.Skus
	for i := 0; i < len(skus); i++ {
		err := track(act.Order.Customer.Id, skus[i].ProductId)
		if err != nil {
			return err
		}
	}

	return nil
}

func track(custId, prodId int) error {
	fmt.Printf("customer %d, product %d\n", custId, prodId)
	return nil
}

func Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
		return err
	}

	if activity.Type() == orderCheckoutCompleted {
		err := parseData(activity.Data())
		if err != nil {
			log.Fatalf("Unable to parse json with error %s", err.Error())
		}
		return err
	}
	return nil
}
