package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShipmentLineItemPayloadTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShipmentLineItemPayloadSuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemPayloadTestSuite))
}

func (suite *ShipmentLineItemPayloadTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentLineItemPayloadTestSuite) Test_ShipmentLineItemDecoding_RunsNormally() {
	//arrange
	referenceNumber := "LI0001"
	sku := "TEST-SKU"
	name := "Test SKU"
	price := uint(4999)
	imagePath := "http://test.com/test.png"
	state := "pending"

	raw := fmt.Sprintf(`{
		"referenceNumber": "%v",
		"sku": "%v",
		"name": "%v",
		"price": %v,
		"imagePath": "%v",
		"state": "%v"
	}`, referenceNumber, sku, name, price, imagePath, state)
	decoder := json.NewDecoder(strings.NewReader(raw))

	//act
	var payload ShipmentLineItem
	err := decoder.Decode(&payload)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(referenceNumber, payload.ReferenceNumber)
	suite.assert.Equal(sku, payload.SKU)
	suite.assert.Equal(name, payload.Name)
	suite.assert.Equal(price, payload.Price)
	suite.assert.Equal(imagePath, payload.ImagePath)
	suite.assert.Equal(state, payload.State)
}
