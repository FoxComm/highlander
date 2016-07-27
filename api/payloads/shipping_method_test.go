package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodPayloadTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShippingMethodPayloadSuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodPayloadTestSuite))
}

func (suite *ShippingMethodPayloadTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodPayloadTestSuite) Test_ShippingMethodDecoding_RunsNormally() {
	//arrange
	carrierID, name := uint(1), "UPS 2 days ground"
	raw := fmt.Sprintf(`{
		"carrierId": %v,
		"name": "%v"
	}`, carrierID, name)
	decoder := json.NewDecoder(strings.NewReader(raw))

	//act
	var payload ShippingMethod
	err := decoder.Decode(&payload)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrierID, payload.CarrierID)
	suite.assert.Equal(name, payload.Name)
}
