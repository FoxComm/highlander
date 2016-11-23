package lib

import (
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers"
)

type PhoenixClient interface {
	Authenticate() exceptions.IException
	CapturePayment(capturePayload *CapturePayload) exceptions.IException
	IsAuthenticated() bool
	UpdateOrder(refNum, shipmentState, orderState string) exceptions.IException
	CreateGiftCards(giftCards []payloads.CreateGiftCardPayload) (*http.Response, exceptions.IException)
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

func (c *phoenixClient) ensureAuthentication() exceptions.IException {
	if c.IsAuthenticated() {
		return nil
	}

	if exception := c.Authenticate(); exception != nil {
		return NewCaptureClientException(fmt.Errorf(
			"Unable to authenticate with %s - cannot proceed with capture",
			exception.ToString(),
		))
	}

	return nil
}

func (c *phoenixClient) CapturePayment(capturePayload *CapturePayload) exceptions.IException {
	if exception := c.ensureAuthentication(); exception != nil {
		return exception
	}

	url := fmt.Sprintf("%s/v1/service/capture", c.baseURL)
	headers := map[string]string{
		"JWT": c.jwt,
	}

	rawCaptureResp, exception := consumers.Post(url, headers, &capturePayload)
	if exception != nil {
		return exception
	}

	defer rawCaptureResp.Body.Close()
	captureResp := new(map[string]interface{})
	if err := json.NewDecoder(rawCaptureResp.Body).Decode(captureResp); err != nil {
		log.Printf("Unable to read capture response from Phoenix with error: %s", err.Error())
		return consumers.NewHttpException(err)
	}

	log.Printf("Successfully captured from Phoenix with response: %v", captureResp)
	log.Printf("Updating order state")

	if exception := c.UpdateOrder(capturePayload.ReferenceNumber, "shipped", "shipped"); exception != nil {
		log.Printf("Enable to update order with error %s", exception.ToString())
		return exception
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

func (c *phoenixClient) Authenticate() exceptions.IException {
	payload := LoginPayload{
		Email:    c.email,
		Password: c.password,
		Org:      "tenant",
	}

	url := fmt.Sprintf("%s/v1/public/login", c.baseURL)
	headers := map[string]string{}

	resp, exception := consumers.Post(url, headers, &payload)
	if exception != nil {
		return NewCaptureClientException(fmt.Errorf("Unable to login: %s", exception.ToString()))
	}

	jwt, ok := resp.Header["Jwt"]
	if !ok {
		return NewCaptureClientException(errors.New("Header with JWT not found in login response"))
	}

	if len(jwt) != 1 {
		return NewCaptureClientException(fmt.Errorf(
			"Unexpected number of values for JWT header -- expected 1, found %d",
			len(jwt),
		))
	}

	c.jwt = jwt[0]

	defer resp.Body.Close()
	loginResp := new(LoginResponse)
	if err := json.NewDecoder(resp.Body).Decode(loginResp); err != nil {
		return consumers.NewHttpException(fmt.Errorf("Error reading login response: %s", err.Error()))
	}

	c.jwtExpiration = loginResp.Expiration

	return nil
}

func (c *phoenixClient) CreateGiftCards(giftCards []payloads.CreateGiftCardPayload) (*http.Response, exceptions.IException) {
	if exception := c.ensureAuthentication(); exception != nil {
		return nil, exception
	}
	url := fmt.Sprintf("%s/v1/customer-gift-cards", c.baseURL)
	headers := map[string]string{
		"JWT": c.jwt,
	}
	return consumers.Post(url, headers, &giftCards)
}

func (c *phoenixClient) UpdateOrder(refNum, shipmentState, orderState string) exceptions.IException {
	if exception := c.ensureAuthentication(); exception != nil {
		return exception
	}

	payload, exception := NewUpdateOrderPayload(orderState)
	if exception != nil {
		return exception
	}

	url := fmt.Sprintf("%s/v1/orders/%s", c.baseURL, refNum)
	headers := map[string]string{
		"JWT": c.jwt,
	}

	rawOrderResp, exception := consumers.Patch(url, headers, &payload)
	if exception != nil {
		return exception
	}

	defer rawOrderResp.Body.Close()
	orderResp := new(map[string]interface{})
	if err := json.NewDecoder(rawOrderResp.Body).Decode(orderResp); err != nil {
		log.Printf("Unable to read order response from Phoenix with error: %s", err.Error())
		return consumers.NewHttpException(err)
	}

	log.Printf("Successfully updated orders in Phoenix  %v", orderResp)

	return nil
}

type captureClientException struct {
	Type string `json:"type"`
	exceptions.Exception
}

func (exception captureClientException) ToJSON() interface{} {
	return exception
}

func NewCaptureClientException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return captureClientException{
		Type:      "captureClient",
		Exception: exceptions.Exception{error.Error()},
	}
}
