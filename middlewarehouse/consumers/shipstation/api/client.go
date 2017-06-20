package api

import (
	"encoding/base64"
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/utils"
)

const baseURL = "https://ssapi.shipstation.com"

// Client in the interface for interacting with ShipStation.
type Client struct {
	httpClient *utils.HTTPClient
}

// NewClient initializes the Client with an API key and secret.
func NewClient(key, secret string) (*Client, error) {
	if key == "" {
		return nil, errors.New("API key must be specified")
	} else if secret == "" {
		return nil, errors.New("API secret must be specified")
	}

	authStr := fmt.Sprintf("%s:%s", key, secret)
	encodedString := base64.StdEncoding.EncodeToString([]byte(authStr))
	authHeader := fmt.Sprintf("Basic %s", encodedString)

	httpClient := utils.NewHTTPClient()
	httpClient.SetHeader("Content-Type", "application/json")
	httpClient.SetHeader("Authorization", authHeader)

	return &Client{httpClient}, nil
}

// CreateOrder creates a new order in ShipStation.
func (c *Client) CreateOrder(payload *payloads.Order) (*payloads.Order, error) {
	order := new(payloads.Order)
	url := fmt.Sprintf("%s/%s", baseURL, "orders/createOrder")
	err := c.httpClient.Post(url, payload, order)
	return order, err
}

func (c *Client) Shipments() (*responses.ShipmentCollection, error) {
	collection := new(responses.ShipmentCollection)
	url := fmt.Sprintf("%s/%s?sortDir=DESC", baseURL, "shipments")
	err := c.httpClient.Get(url, collection)
	return collection, err
}
