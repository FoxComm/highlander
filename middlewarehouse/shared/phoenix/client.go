package phoenix

import (
	"errors"
	"fmt"
	"log"
	"time"

	mwhPayloads "github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
)

type PhoenixClient interface {
	GetJwt() string
	Authenticate() error
	IsAuthenticated() bool
	EnsureAuthentication() error
	CapturePayment(capturePayload *payloads.CapturePayload) error
	UpdateOrder(refNum, shipmentState, orderState string) error
	CreateGiftCards(giftCards []mwhPayloads.CreateGiftCardPayload) ([]*mwhPayloads.GiftCardResponse, error)
	GetOrder(refNum string) (*mwhPayloads.OrderResult, error)
	UpdateOrderLineItems(updatePayload []mwhPayloads.UpdateOrderLineItem, refNum string) error
	GetCustomerGroups() ([]*responses.CustomerGroupResponse, error)
	UpdateCustomerGroup(groupID int, group *payloads.CustomerGroupPayload) error
	SetCustomersToGroup(groupID int, customers []int) error
	GetPlugins() ([]*responses.PluginResponse, error)
	GetPluginSettings(name string) (map[string]interface{}, error)
}

func NewPhoenixClient(baseURL, email, password string) PhoenixClient {
	return &phoenixClient{
		baseURL:  baseURL,
		email:    email,
		password: password,
	}
}

type phoenixClient struct {
	baseURL       string
	jwt           string
	jwtExpiration int64
	email         string
	password      string
}

func (c *phoenixClient) defaultHeaders() map[string]string {
	return map[string]string{
		"JWT": c.jwt,
	}
}

func (c *phoenixClient) GetJwt() string {
	return c.jwt
}

// Authenticate
func (c *phoenixClient) Authenticate() error {
	payload := payloads.LoginPayload{
		Email:    c.email,
		Password: c.password,
		Org:      "tenant",
	}

	url := fmt.Sprintf("%s/v1/public/login", c.baseURL)

	loginResp := new(responses.LoginResponse)
	resp, err := consumers.Request("POST", url, nil, &payload, loginResp)
	if err != nil {
		return fmt.Errorf("Unable to login: %s", err.Error())
	}

	jwt, ok := resp.Header["Jwt"]
	if !ok {
		return errors.New("Header with JWT not found in login response")
	}

	if len(jwt) != 1 {
		return fmt.Errorf(
			"Unexpected number of values for JWT header -- expected 1, found %d",
			len(jwt),
		)
	}

	c.jwt = jwt[0]
	c.jwtExpiration = loginResp.Expiration

	return nil
}

// IsAuthenticated
func (c *phoenixClient) IsAuthenticated() bool {
	if c.jwt == "" {
		return false
	}

	currentUnix := time.Now().Unix()
	if currentUnix > c.jwtExpiration {
		return false
	}

	return true
}

func (c *phoenixClient) EnsureAuthentication() error {
	if c.IsAuthenticated() {
		return nil
	}

	if err := c.Authenticate(); err != nil {
		return fmt.Errorf("Unable to authenticate with Phoenix with error %s", err.Error())
	}

	return nil
}

// CapturePayment
func (c *phoenixClient) CapturePayment(capturePayload *payloads.CapturePayload) error {
	if err := c.EnsureAuthentication(); err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/service/capture", c.baseURL)

	captureResp := new(map[string]interface{})
	err := consumers.Post(url, c.defaultHeaders(), &capturePayload, captureResp)
	if err != nil {
		return err
	}

	log.Printf("Successfully captured from Phoenix with response: %v", captureResp)
	log.Println("Updating order state")

	if err := c.UpdateOrder(capturePayload.ReferenceNumber, "shipped", "shipped"); err != nil {
		log.Printf("Enable to update order with error %s", err.Error())
		return err
	}

	return nil
}

// CreateGiftCards
func (c *phoenixClient) CreateGiftCards(giftCards []mwhPayloads.CreateGiftCardPayload) ([]*mwhPayloads.GiftCardResponse, error) {
	if err := c.EnsureAuthentication(); err != nil {
		return nil, err
	}
	url := fmt.Sprintf("%s/v1/customer-gift-cards", c.baseURL)

	resp := []*mwhPayloads.GiftCardResponse{}

	err := consumers.Post(url, c.defaultHeaders(), &giftCards, &resp)

	return resp, err
}

// GetOrder
func (c *phoenixClient) GetOrder(refNum string) (*mwhPayloads.OrderResult, error) {
	if err := c.EnsureAuthentication(); err != nil {
		return nil, err
	}

	url := fmt.Sprintf("%s/v1/orders/%s", c.baseURL, refNum)

	orderResp := new(mwhPayloads.OrderResult)
	err := consumers.Get(url, c.defaultHeaders(), orderResp)
	if err != nil {
		return nil, err
	}

	log.Printf("Successfully fetched order %s from Phoenix", refNum)
	return orderResp, nil
}

// UpdateOrder
func (c *phoenixClient) UpdateOrder(refNum, shipmentState, orderState string) error {
	if err := c.EnsureAuthentication(); err != nil {
		return err
	}

	payload, err := payloads.NewUpdateOrderPayload(orderState)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/orders/%s", c.baseURL, refNum)

	orderResp := new(map[string]interface{})
	err = consumers.Patch(url, c.defaultHeaders(), &payload, orderResp)
	if err != nil {
		return err
	}

	log.Printf("Successfully updated orders in Phoenix %v", orderResp)

	return nil
}

func (c *phoenixClient) UpdateOrderLineItems(updatePayload []mwhPayloads.UpdateOrderLineItem, refNum string) error {
	if err := c.EnsureAuthentication(); err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/orders/%s/order-line-items", c.baseURL, refNum)

	orderResp := new(map[string]interface{})
	err := consumers.Patch(url, c.defaultHeaders(), &updatePayload, orderResp)
	if err != nil {
		return err
	}

	return nil
}

func (c *phoenixClient) GetCustomerGroups() ([]*responses.CustomerGroupResponse, error) {
	if err := c.EnsureAuthentication(); err != nil {
		return nil, err
	}

	url := fmt.Sprintf("%s/v1/service/customer-groups", c.baseURL)

	groups := []*responses.CustomerGroupResponse{}
	err := consumers.Get(url, c.defaultHeaders(), &groups)
	if err != nil {
		return nil, err
	}

	return groups, nil
}

func (c *phoenixClient) UpdateCustomerGroup(groupID int, group *payloads.CustomerGroupPayload) error {
	if err := c.EnsureAuthentication(); err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/customer-groups/%d", c.baseURL, groupID)

	err := consumers.Patch(url, c.defaultHeaders(), group, nil)

	return err
}

func (c *phoenixClient) SetCustomersToGroup(groupID int, customerIDs []int) error {
	if err := c.EnsureAuthentication(); err != nil {
		return err
	}

	payload := struct {
		Customers []int `json:"customers"`
	}{
		Customers: customerIDs,
	}

	url := fmt.Sprintf("%s/v1/service/customer-groups/%d/customers", c.baseURL, groupID)

	err := consumers.Post(url, c.defaultHeaders(), payload, nil)

	return err
}

func (c *phoenixClient) GetPlugins() ([]*responses.PluginResponse, error) {
	if err := c.EnsureAuthentication(); err != nil {
		return nil, err
	}

	url := fmt.Sprintf("%s/v1/plugins", c.baseURL)

	plugins := []*responses.PluginResponse{}
	err := consumers.Get(url, c.defaultHeaders(), &plugins)
	if err != nil {
		return nil, err
	}

	return plugins, nil
}

func (c *phoenixClient) GetPluginSettings(name string) (map[string]interface{}, error) {
	if err := c.EnsureAuthentication(); err != nil {
		return nil, err
	}

	url := fmt.Sprintf("%s/v1/plugins/settings/%s", c.baseURL, name)

	settings := map[string]interface{}{}
	err := consumers.Get(url, c.defaultHeaders(), &settings)
	if err != nil {
		return nil, err
	}

	return settings, nil
}
