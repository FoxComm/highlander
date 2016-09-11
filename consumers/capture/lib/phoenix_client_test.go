package lib

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/suite"
)

type PhoenixClientTestSuite struct {
	suite.Suite
}

func TestPhoenixClientSuite(t *testing.T) {
	suite.Run(t, new(PhoenixClientTestSuite))
}

func (suite *PhoenixClientTestSuite) TestAuthenticate() {
	fp := fakePhoenix{"admin@admin.com", "password", suite}
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	client := NewPhoenixClient(ts.URL, "admin@admin.com", "password")
	err := client.Authenticate()
	suite.Nil(err)
}

type fakePhoenix struct {
	username string
	password string
	suite    *PhoenixClientTestSuite
}

func (fp *fakePhoenix) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Path

	switch url {
	case "/v1/public/login":
		fp.handleLogin(w, r)
	default:
		msg := fmt.Sprintf("Invalid route %s", url)
		fp.suite.Fail(msg)
	}
}

func (fp *fakePhoenix) handleLogin(w http.ResponseWriter, r *http.Request) {
	payload := new(LoginPayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.Email, fp.username)
	fp.suite.Equal(payload.Password, fp.password)
	fp.suite.Equal(payload.Kind, "admin")

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("JWT", "FAKEJWT")

	resp := LoginResponse{
		ID:         1,
		Email:      "admin@admin.com",
		Ratchet:    0,
		Name:       "Frankly Admin",
		Expiration: 1473726411,
		Issuer:     "FC",
		IsAdmin:    true,
	}

	respBytes, err := json.Marshal(resp)
	fp.suite.Nil(err)
	w.Write(respBytes)
}
