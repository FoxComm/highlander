package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/stretchr/testify/suite"
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

func (suite *ConsumerTestSuite) TestMessageHander() {
	msg := testAvroMessage{
		b: []byte(`{"id": 1, "sku_code": "SKU-HANDLER"}`),
	}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		uri := r.URL.Path
		suite.Equal("/stock-items", uri)

		var stockItem models.StockItem
		err := json.NewDecoder(r.Body).Decode(&stockItem)
		suite.Nil(err)
		suite.Equal("SKU-HANDLER", stockItem.SKU)
	}))
	defer ts.Close()

	consumer, err := NewConsumer("localhost:2181", "http://localhost:8081", ts.URL)
	suite.Nil(err)

	err = consumer.handler(msg)
	suite.Nil(err)
}
