package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type AddressPayloadTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestAddressPayloadSuite(t *testing.T) {
	suite.Run(t, new(AddressPayloadTestSuite))
}

func (suite *AddressPayloadTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *AddressPayloadTestSuite) Test_AddressDecoding_RunsNormally() {
	//arrange
	name := "Home address"
	regionID := uint(1)
	city := "Seattle"
	zip := "71234"
	address1 := "Some st, 51"
	address2 := "Some more here"
	phoneNumber := "17345791232"

	raw := fmt.Sprintf(`{
		"name": "%v",
		"regionId": %v,
		"city": "%v",
		"zip": "%v",
		"address1": "%v",
		"address2": "%v",
		"phoneNumber": "%v"
	}`, name, regionID, city, zip, address1, address2, phoneNumber)
	decoder := json.NewDecoder(strings.NewReader(raw))

	//act
	var payload Address
	err := decoder.Decode(&payload)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(name, payload.Name)
	suite.assert.Equal(regionID, payload.RegionID)
	suite.assert.Equal(city, payload.City)
	suite.assert.Equal(zip, payload.Zip)
	suite.assert.Equal(address1, payload.Address1)
	suite.assert.Equal(address2, payload.Address2)
	suite.assert.Equal(phoneNumber, payload.PhoneNumber)
}
