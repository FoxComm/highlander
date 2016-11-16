package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/stretchr/testify/suite"
)

const (
	username = "admin@admin.com"
	password = "password"
)

type PhoenixClientTestSuite struct {
	suite.Suite
}

func TestPhoenixClientSuite(t *testing.T) {
	suite.Run(t, new(PhoenixClientTestSuite))
}

func (suite *PhoenixClientTestSuite) TestAuthenticate() {
	authExpires := time.Now().AddDate(0, 0, 1)

	fp := newFakePhoenix(username, password, authExpires, "", suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	client := lib.NewPhoenixClient(ts.URL, username, password)
	err := client.Authenticate()
	suite.Nil(err)
	suite.True(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestNotAuthedWithOldExpiration() {
	authExpires := time.Now().AddDate(0, 0, -1)

	fp := newFakePhoenix(username, password, authExpires, "", suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	client := lib.NewPhoenixClient(ts.URL, username, password)
	err := client.Authenticate()
	suite.Nil(err)
	suite.False(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestNotAuthedOnInit() {
	client := lib.NewPhoenixClient("http://test.com", "", "")
	suite.False(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestCapture() {
	authExpires := time.Now().AddDate(0, 0, 1)

	shipment := fixtures.GetShipmentShort(1)
	fp := newFakePhoenix(username, password, authExpires, shipment.ReferenceNumber, suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	activity, err := activities.NewShipmentShipped(shipment, time.Now())

	client := lib.NewPhoenixClient(ts.URL, username, password)
	payload, _ := lib.NewCapturePayload(activity)
	err = client.CapturePayment(payload)
	suite.Nil(err)

	suite.True(fp.LoginCalled)
	suite.True(fp.CaptureCalled)
	suite.True(fp.UpdateOrderCalled)
}

type fakePhoenix struct {
	username    string
	password    string
	authExpires time.Time
	refNum      string
	suite       *PhoenixClientTestSuite

	LoginCalled       bool
	CaptureCalled     bool
	UpdateOrderCalled bool
}

func newFakePhoenix(
	username string,
	password string,
	authExpires time.Time,
	refNum string,
	suite *PhoenixClientTestSuite) *fakePhoenix {

	return &fakePhoenix{username, password, authExpires, refNum, suite, false, false, false}
}

func (fp *fakePhoenix) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	url := r.URL.Path
	updateURI := fmt.Sprintf("/v1/orders/%s", fp.refNum)

	switch url {
	case "/v1/public/login":
		fp.handleLogin(w, r)
	case "/v1/service/capture":
		fp.handleCapture(w, r)
	case updateURI:
		fp.handleUpdateOrder(w, r)
	default:
		msg := fmt.Sprintf("Invalid route %s", url)
		fp.suite.Fail(msg)
	}
}

func (fp *fakePhoenix) handleCapture(w http.ResponseWriter, r *http.Request) {
	fp.CaptureCalled = true

	payload := new(lib.CapturePayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.ReferenceNumber, fp.refNum)

	msg := map[string]string{"msg": "success"}
	respBytes, err := json.Marshal(msg)
	fp.suite.Nil(err)
	w.Write(respBytes)
}

func (fp *fakePhoenix) handleLogin(w http.ResponseWriter, r *http.Request) {
	fp.LoginCalled = true

	payload := new(lib.LoginPayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.Email, fp.username)
	fp.suite.Equal(payload.Password, fp.password)
	fp.suite.Equal(payload.Org, "tenant")

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("JWT", "FAKEJWT")

	resp := lib.LoginResponse{
		ID:         1,
		Email:      "admin@admin.com",
		Ratchet:    0,
		Name:       "Frankly Admin",
		Expiration: fp.authExpires.Unix(),
		Issuer:     "FC",
		IsAdmin:    true,
	}

	respBytes, err := json.Marshal(resp)
	fp.suite.Nil(err)
	w.Write(respBytes)
}

func (fp *fakePhoenix) handleUpdateOrder(w http.ResponseWriter, r *http.Request) {
	fp.UpdateOrderCalled = true

	payload := new(lib.UpdateOrderPayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.State, "shipped")

	msg := map[string]string{"msg": "success"}
	respBytes, err := json.Marshal(msg)
	fp.suite.Nil(err)
	w.Write(respBytes)
}
