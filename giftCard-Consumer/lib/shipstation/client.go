package shipstation

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/FoxComm/shipstation/lib/shipstation/payloads"
	"github.com/FoxComm/shipstation/lib/shipstation/responses"
)

// Client in the interface for interacting with ShipStation.
type Client struct {
	authHeader string
}

// NewClient initializes the Client with an API key and secret.
func NewClient(key, secret string) (*Client, error) {
	if key == "" {
		return nil, errors.New("API key must be specified")
	} else if secret == "" {
		return nil, errors.New("API secret must be specifid")
	}

	authStr := fmt.Sprintf("%s:%s", key, secret)
	authHeader := base64.StdEncoding.EncodeToString([]byte(authStr))

	return &Client{authHeader: fmt.Sprintf("Basic %s", authHeader)}, nil
}

func (c *Client) getRequest(url string, resp interface{}) error {
	return c.request("GET", url, nil, resp)
}

func (c *Client) postRequest(url string, payload interface{}, resp interface{}) error {
	return c.request("POST", url, payload, resp)
}

func (c *Client) putRequest(url string, payload interface{}, resp interface{}) error {
	return c.request("PUT", url, payload, resp)
}

func (c *Client) request(method string, url string, payload interface{}, respBody interface{}) error {
	client := &http.Client{}
	body := new(bytes.Buffer)

	if method != "GET" {
		if err := json.NewEncoder(body).Encode(payload); err != nil {
			return err
		}
	}

	req, err := http.NewRequest(method, url, body)
	if err != nil {
		return err
	}

	req.Header.Add("Authorization", c.authHeader)
	req.Header.Add("Content-Type", "application/json")

	resp, err := client.Do(req)
	if err != nil {
		return err
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {

		errResp, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return err
		}

		return errors.New(string(errResp))
	}

	if err := json.NewDecoder(resp.Body).Decode(respBody); err != nil {
		return err
	}

	return nil
}

func (c *Client) CreateOrder(payload *payloads.Order) (*payloads.Order, error) {
	order := new(payloads.Order)
	err := c.postRequest("https://ssapi.shipstation.com/orders/createOrder", payload, order)
	return order, err
}

// Products retreives a paginated list of all products in ShipStation.
func (c *Client) Products() (*responses.ProductCollection, error) {
	collection := new(responses.ProductCollection)
	err := c.getRequest("https://ssapi.shipstation.com/products", collection)
	return collection, err
}

// Product retreives a product from ShipStation.
func (c *Client) Product(id int) (*responses.Product, error) {
	product := new(responses.Product)
	url := fmt.Sprintf("https://ssapi.shipstation.com/products/%d", id)
	err := c.getRequest(url, product)
	return product, err
}

// UpdateProduct updates an existing ShipStation product.
func (c *Client) UpdateProduct(payload *payloads.Product) (*responses.Product, error) {
	product := new(responses.Product)
	url := fmt.Sprintf("https://ssapi.shipstation.com/products/%d", payload.ID)
	err := c.putRequest(url, payload, product)
	return product, err
}
