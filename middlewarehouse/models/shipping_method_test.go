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

func (suite *ShippingMethodModelTestSuite) Test_ShippingMethodPayloadToModel_ReturnsValidModel() {
	//arrange
	carrierID, name, code, shippingType, cost, scope := uint(1), "UPS 2 days ground", "GROUND", "flat", uint(599), payloads.Scopable{"1.2"}
	payload := &payloads.ShippingMethod{carrierID, name, code, shippingType, cost, scope}

	//act
	model, err := payload.Model()
	suite.assert.Nil(err)

	//assert
	suite.assert.Equal(carrierID, model.CarrierID)
	suite.assert.Equal(name, model.Name)
}
