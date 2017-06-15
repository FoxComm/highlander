package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"time"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/stretchr/testify/suite"
)

const (
	username = "admin@admin.com"
	password = "password"
)

type ConsumerTestSuite struct {
	suite.Suite
}

type testAvroMessage struct {
	b []byte
}

func (t testAvroMessage) Bytes() []byte {
	return t.b
}

func TestConsumerSuite(t *testing.T) {
	suite.Run(t, new(ConsumerTestSuite))
}

func (suite *ConsumerTestSuite) TestMessageHandler() {
	msg := testAvroMessage{
		b: []byte(`{"id": 1, "sku_code": "SKU-HANDLER"}`),
	}

	authExpires := time.Now().AddDate(0, 0, 1)

	fp := phoenix.NewFakePhoenix(username, password, authExpires, "", &suite.Suite)
	tss := httptest.NewServer(http.HandlerFunc(fp.ServeHTTP))
	defer tss.Close()

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		uri := r.URL.Path
		suite.Equal("/v1/public/skus", uri)

		var sku payloads.CreateSKU
		err := json.NewDecoder(r.Body).Decode(&sku)
		suite.Nil(err)
		suite.Equal("SKU-HANDLER", sku.Code)
	}))
	defer ts.Close()

	client := phoenix.NewPhoenixClient(tss.URL, username, password)
	consumer, err := NewStockItemsConsumer(client, ts.URL)
	suite.Nil(err)

	err = consumer.Handler(msg)
	suite.Nil(err)
}
