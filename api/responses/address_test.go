package responses

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type AddressResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestAddressResponseSuite(t *testing.T) {
	suite.Run(t, new(AddressResponseTestSuite))
}

func (suite *AddressResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *AddressResponseTestSuite) Test_NewAddressFromModel_ReturnsValidResponse() {
	//arrange
	id := uint(1)
	name := "Home address"
	regionID := uint(1)
	city := "Seattle"
	zip := "71234"
	address1 := "Some st, 51"
	address2 := "Some more here"
	phoneNumber := "17345791232"

	model := &models.Address{gormfox.Base{}, name, regionID, city, zip, address1, sql.NullString{address2, true}, phoneNumber}
	model.ID = id

	//act
	response := NewAddressFromModel(model)

	//assert
	suite.assert.Equal(id, response.ID)
	suite.assert.Equal(name, response.Name)
	suite.assert.Equal(city, response.City)
	suite.assert.Equal(zip, response.Zip)
	suite.assert.Equal(address1, response.Address1)
	suite.assert.Equal(address2, *response.Address2)
	suite.assert.Equal(phoneNumber, response.PhoneNumber)
}

func (suite *AddressResponseTestSuite) Test_AddressEncoding_RunsNormally() {
	//arrange
	id := uint(1)
	name := "Home address"
	region := Region{uint(1), "Texas", uint(2), "USA"}
	city := "Seattle"
	zip := "71234"
	address1 := "Some st, 51"
	address2 := "Some more here"
	phoneNumber := "17345791232"

	response := &Address{id, name, region, city, zip, address1, &address2, phoneNumber}
	expected := fmt.Sprintf(
		`{"id":%v,"name":"%v","region":{"id":%v,"name":"%v","countryId":%v,"countryName":"%v"},"city":"%v","zip":"%v","address1":"%v","address2":"%v","phoneNumber":"%v"}`,
		id, name, region.ID, region.Name, region.CountryID, region.CountryName, city, zip, address1, address2, phoneNumber)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
