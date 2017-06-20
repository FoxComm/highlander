package suggester_phoenix

import (
	"errors"
	"fmt"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/lib/gohttp"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
)

type SuggesterPhoenixClient interface {
	GetJwt() string
	Authenticate() error
	IsAuthenticated() bool
	EnsureAuthentication() error
	GetBaseURL() string
	GetDefaultHeaders() map[string]string
}

func NewPhoenixClient(baseURL, email, password string) SuggesterPhoenixClient {
	return &suggesterPhoenixClient{
		baseURL:  baseURL,
		email:    email,
		password: password,
	}
}

type suggesterPhoenixClient struct {
	baseURL       string
	jwt           string
	jwtExpiration int64
	email         string
	password      string
}

func (c *suggesterPhoenixClient) GetDefaultHeaders() map[string]string {
	return map[string]string{
		"JWT": c.jwt,
	}
}

func (c *suggesterPhoenixClient) GetJwt() string {
	return c.jwt
}

func (c *suggesterPhoenixClient) GetBaseURL() string {
	return c.baseURL
}

// Authenticate
func (c *suggesterPhoenixClient) Authenticate() error {
	payload := payloads.LoginPayload{
		Email:    c.email,
		Password: c.password,
		Org:      "tenant",
	}

	url := fmt.Sprintf("%s/v1/public/login", c.baseURL)

	loginResp := new(responses.LoginResponse)
	resp, err := gohttp.Request("POST", url, nil, &payload, loginResp)
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
func (c *suggesterPhoenixClient) IsAuthenticated() bool {
	if c.jwt == "" {
		return false
	}

	currentUnix := time.Now().Unix()
	if currentUnix > c.jwtExpiration {
		return false
	}

	return true
}

func (c *suggesterPhoenixClient) EnsureAuthentication() error {
	if c.IsAuthenticated() {
		return nil
	}

	if err := c.Authenticate(); err != nil {
		return fmt.Errorf("Unable to authenticate with Phoenix with error %s", err.Error())
	}

	return nil
}
