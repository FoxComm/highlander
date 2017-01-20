package phoenix

import (
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/suite"
	"net/http"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
)

type FakePhoenix struct {
	username    string
	password    string
	authExpires time.Time
	refNum      string
	suite       *suite.Suite

	LoginCalled       bool
	CaptureCalled     bool
	UpdateOrderCalled bool
}

func NewFakePhoenix(
	username string,
	password string,
	authExpires time.Time,
	refNum string,
	suite *suite.Suite) *FakePhoenix {

	return &FakePhoenix{username, password, authExpires, refNum, suite, false, false, false}
}

func (fp *FakePhoenix) ServeHTTP(w http.ResponseWriter, r *http.Request) {
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

func (fp *FakePhoenix) handleCapture(w http.ResponseWriter, r *http.Request) {
	fp.CaptureCalled = true

	payload := new(payloads.CapturePayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.ReferenceNumber, fp.refNum)

	msg := map[string]string{"msg": "success"}
	respBytes, err := json.Marshal(msg)
	fp.suite.Nil(err)
	w.Write(respBytes)
}

func (fp *FakePhoenix) handleLogin(w http.ResponseWriter, r *http.Request) {
	fp.LoginCalled = true

	payload := new(payloads.LoginPayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.Email, fp.username)
	fp.suite.Equal(payload.Password, fp.password)
	fp.suite.Equal(payload.Org, "tenant")

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("JWT", "FAKEJWT")

	resp := responses.LoginResponse{
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

func (fp *FakePhoenix) handleUpdateOrder(w http.ResponseWriter, r *http.Request) {
	fp.UpdateOrderCalled = true

	payload := new(payloads.UpdateOrderPayload)

	defer r.Body.Close()
	err := json.NewDecoder(r.Body).Decode(payload)
	fp.suite.Nil(err)
	fp.suite.Equal(payload.State, "shipped")

	msg := map[string]string{"msg": "success"}
	respBytes, err := json.Marshal(msg)
	fp.suite.Nil(err)
	w.Write(respBytes)
}
