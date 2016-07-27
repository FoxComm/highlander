package models

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type AddressModelTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestAddressModelSuite(t *testing.T) {
	suite.Run(t, new(AddressModelTestSuite))
}

func (suite *AddressModelTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *AddressModelTestSuite) Test_NewAddressFromPayload_ReturnsValidModel() {
	//arrange
	name := "Home address"
	regionID := uint(1)
	city := "Seattle"
	zip := "71234"
	address1 := "Some st, 51"
	address2 := "Some more here"
	phoneNumber := "17345791232"

	payload := &payloads.Address{name, regionID, city, zip, address1, &address2, phoneNumber}

	//act
	model := NewAddressFromPayload(payload)

	//assert
	suite.assert.Equal(name, model.Name)
	suite.assert.Equal(regionID, model.RegionID)
	suite.assert.Equal(city, model.City)
	suite.assert.Equal(zip, model.Zip)
	suite.assert.Equal(address1, model.Address1)
	suite.assert.Equal(address2, model.Address2.String)
	suite.assert.True(model.Address2.Valid)
	suite.assert.Equal(phoneNumber, model.PhoneNumber)
}
