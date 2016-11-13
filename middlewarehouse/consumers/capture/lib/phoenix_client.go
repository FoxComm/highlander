package lib

import (
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/FoxComm/highlander/shared/golang/activities"
	"github.com/FoxComm/highlander/shared/golang/http"
)

type PhoenixClient interface {
	Authenticate() error
	CapturePayment(activities.SiteActivity) error
	IsAuthenticated() bool
	UpdateOrder(refNum, shipmentState, orderState string) error
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

func (c *phoenixClient) ensureAuthentication() error {
	if c.IsAuthenticated() {
		return nil
	}

	if err := c.Authenticate(); err != nil {
		return fmt.Errorf(
			"Unable to authenticate with %s - cannot proceed with capture",
			err.Error(),
		)
	}

	return nil
}

func (c *phoenixClient) CapturePayment(activity activities.SiteActivity) error {
	if err := c.ensureAuthentication(); err != nil {
		return err
	}

	capture, err := NewCapturePayload(activity)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/service/capture", c.baseURL)
	headers := map[string]string{
		"JWT": c.jwt,
	}

	rawCaptureResp, err := http.Post(url, headers, &capture)
	if err != nil {
		return err
	}

	defer rawCaptureResp.Body.Close()
	captureResp := new(map[string]interface{})
	if err := json.NewDecoder(rawCaptureResp.Body).Decode(captureResp); err != nil {
		log.Printf("Unable to read capture response from Phoenix with error: %s", err.Error())
		return err
	}

	log.Printf("Successfully captured from Phoenix with response: %v", captureResp)
	log.Printf("Updating order state")

	if err := c.UpdateOrder(capture.ReferenceNumber, "shipped", "shipped"); err != nil {
		log.Printf("Enable to update order with error %s", err.Error())
		return err
	}

	return nil
}

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

func (c *phoenixClient) Authenticate() error {
	payload := LoginPayload{
		Email:    c.email,
		Password: c.password,
		Org:      "tenant",
	}

	url := fmt.Sprintf("%s/v1/public/login", c.baseURL)
	headers := map[string]string{}

	resp, err := http.Post(url, headers, &payload)
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

	defer resp.Body.Close()
	loginResp := new(LoginResponse)
	if err := json.NewDecoder(resp.Body).Decode(loginResp); err != nil {
		return fmt.Errorf("Error reading login response: %s", err.Error())
	}

	c.jwtExpiration = loginResp.Expiration

	return nil
}

func (c *phoenixClient) UpdateOrder(refNum, shipmentState, orderState string) error {
	if err := c.ensureAuthentication(); err != nil {
		return err
	}

	payload, err := NewUpdateOrderPayload(orderState)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/orders/%s", c.baseURL, refNum)
	headers := map[string]string{
		"JWT": c.jwt,
	}

	rawOrderResp, err := http.Patch(url, headers, &payload)
	if err != nil {
		return err
	}

	defer rawOrderResp.Body.Close()
	orderResp := new(map[string]interface{})
	if err := json.NewDecoder(rawOrderResp.Body).Decode(orderResp); err != nil {
		log.Printf("Unable to read order response from Phoenix with error: %s", err.Error())
		return err
	}

	log.Printf("Successfully updated orders in Phoenix with error: %v", orderResp)

	return nil
}
