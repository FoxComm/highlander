package lib

import (
	"errors"
	"fmt"

	"github.com/FoxComm/middlewarehouse/consumers"
	"github.com/FoxComm/middlewarehouse/models/activities"
)

type PhoenixClient interface {
	Authenticate() error
	CapturePayment(activities.ISiteActivity) error
}

func NewPhoenixClient(baseURL, email, password string) PhoenixClient {
	return &phoenixClient{
		baseURL:  baseURL,
		email:    email,
		password: password,
	}
}

type phoenixClient struct {
	baseURL  string
	jwt      string
	email    string
	password string
}

func (c *phoenixClient) CapturePayment(activity activities.ISiteActivity) error {
	return nil
}

func (c *phoenixClient) Authenticate() error {
	payload := LoginPayload{
		Email:    c.email,
		Password: c.password,
		Kind:     "admin",
	}

	url := fmt.Sprintf("%s/v1/public/login", c.baseURL)
	headers := map[string]string{}

	resp, err := consumers.Post(url, headers, &payload)
	if err != nil {
		return fmt.Errorf("Unable to capture payment: %s", err.Error())
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

	return nil
}
