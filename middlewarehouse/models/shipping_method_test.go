package models

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodModelTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShippingMethodModelSuite(t *testing.T) {
	suite.Run(t, new(suite.Suite))
}

func (suite *ShippingMethodModelTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodModelTestSuite) Test_NewShippingMethodFromPayload_ReturnsValidModel() {
	//arrange
	carrierID, name, code, shippingDays := uint(1), "UPS 2 days ground", "GROUND", 2
	payload := &payloads.ShippingMethod{carrierID, name, code, shippingDays}

	//act
	model := NewShippingMethodFromPayload(payload)

	//assert
	suite.assert.Equal(carrierID, model.CarrierID)
	suite.assert.Equal(name, model.Name)
}
