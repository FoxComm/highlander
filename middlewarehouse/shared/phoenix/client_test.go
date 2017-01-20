package phoenix

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
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

	fp := NewFakePhoenix(username, password, authExpires, "", &suite.Suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	client := NewPhoenixClient(ts.URL, username, password)
	err := client.Authenticate()
	suite.Nil(err)
	suite.True(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestNotAuthedWithOldExpiration() {
	authExpires := time.Now().AddDate(0, 0, -1)

	fp := NewFakePhoenix(username, password, authExpires, "", &suite.Suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	client := NewPhoenixClient(ts.URL, username, password)
	err := client.Authenticate()
	suite.Nil(err)
	suite.False(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestNotAuthedOnInit() {
	client := NewPhoenixClient("http://test.com", "", "")
	suite.False(client.IsAuthenticated())
}

func (suite *PhoenixClientTestSuite) TestCapture() {
	authExpires := time.Now().AddDate(0, 0, 1)

	shipment := fixtures.GetShipmentShort(1)
	fp := NewFakePhoenix(username, password, authExpires, shipment.ReferenceNumber, &suite.Suite)
	ts := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer ts.Close()

	activity, err := activities.NewShipmentShipped(shipment, time.Now())

	client := NewPhoenixClient(ts.URL, username, password)
	payload, _ := payloads.NewCapturePayload(activity)
	err = client.CapturePayment(payload)
	suite.Nil(err)

	suite.True(fp.LoginCalled)
	suite.True(fp.CaptureCalled)
	suite.True(fp.UpdateOrderCalled)
}
