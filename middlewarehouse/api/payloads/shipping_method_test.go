package payloads

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodPayloadTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShippingMethodPayloadSuite(t *testing.T) {
	suite.Run(t, new(suite.Suite))
}

func (suite *ShippingMethodPayloadTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodPayloadTestSuite) Test_ShippingMethodPayloadToModel_ReturnsValidModel() {
	//arrange
	carrierID, name, code, shippingType, cost, scope := uint(1), "UPS 2 days ground", "GROUND", "flat", uint(599), Scopable{"1.2"}
	payload := &ShippingMethod{carrierID, name, code, shippingType, cost, scope}

	//act
	model, err := payload.Model()
	suite.assert.Nil(err)

	//assert
	suite.assert.Equal(carrierID, model.CarrierID)
	suite.assert.Equal(name, model.Name)
}
