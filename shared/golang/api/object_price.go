package api

import (
	"errors"
	"fmt"
)

type ObjectPrice struct {
	Value    int
	Currency string
}

func NewObjectPrice(attr ObjectAttribute) (*ObjectPrice, error) {
	if attr.Type != "price" {
		return nil, fmt.Errorf("Expected type price but got %s", attr.Type)
	}

	v := attr.Value.(map[string]interface{})
	price, ok := v["value"]
	if !ok {
		return nil, errors.New("Unable to get the value")
	}

	currency, ok := v["currency"]
	if !ok {
		return nil, errors.New("Unable to get the currency")
	}

	return &ObjectPrice{
		Value:    int(price.(float64)),
		Currency: currency.(string),
	}, nil
}
