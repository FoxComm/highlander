package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

type skus []struct {
	productFormId int64 `json:"productFormId"`
}

type lineItems struct {
	skus skus `json:"skus"`
}

type customer struct {
	id int64 `json:"id"`
}

type order struct {
	customer  customer  `json:"customer"`
	lineItems lineItems `json:"lineItems"`
}

type activity struct {
	order order `json:"cart"`
}

func Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
	}

	if activity.Type() == orderCheckoutCompleted {
		fmt.Println(activity.Data())
	}
	return nil
}
