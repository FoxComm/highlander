package shipstation

import (
	"encoding/base64"
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation/payloads"
	"github.com/FoxComm/highlander/integrations/shipstation/lib/shipstation/responses"
	"github.com/FoxComm/highlander/integrations/shipstation/utils"
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
		return nil, errors.New("API secret must be specifid")
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

// Products retreives a paginated list of all products in ShipStation.
func (c *Client) Products() (*responses.ProductCollection, error) {
	collection := new(responses.ProductCollection)
	url := fmt.Sprintf("%s/%s", baseURL, "products")
	err := c.httpClient.Get(url, collection)
	return collection, err
}

// Product retreives a product from ShipStation.
func (c *Client) Product(id int) (*responses.Product, error) {
	product := new(responses.Product)
	url := fmt.Sprintf("%s/%d", baseURL, id)
	err := c.httpClient.Get(url, product)
	return product, err
}

// UpdateProduct updates an existing ShipStation product.
func (c *Client) UpdateProduct(payload *payloads.Product) (*responses.Product, error) {
	product := new(responses.Product)
	url := fmt.Sprintf("%s/%d", baseURL, payload.ID)
	err := c.httpClient.Put(url, payload, product)
	return product, err
}
