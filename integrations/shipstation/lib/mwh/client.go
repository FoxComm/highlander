package mwh

import (
	"fmt"

	"github.com/FoxComm/highlander/integrations/shipstation/utils"
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
)

const baseURL = "http://localhost:9292"

// Client in the interface for interacting with middlewarehouse.
type Client struct {
	httpClient *utils.HTTPClient
}

// NewClient initializes the Client.
func NewClient() *Client {
	httpClient := utils.NewHTTPClient()
	httpClient.SetHeader("Content-Type", "application/json")
	return &Client{httpClient}
}

// CreateShipment creates a new shipment in middlewarehouse.
func (c *Client) CreateShipment(payload *payloads.Shipment) (*payloads.Shipment, error) {
	shipment := new(payloads.Shipment)
	url := fmt.Sprintf("%s/shipments", baseURL)
	err := c.httpClient.Post(url, payload, shipment)
	return shipment, err
}
