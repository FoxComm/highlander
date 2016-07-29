package responses

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type stockLocationResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestStockLocationResponseSuite(t *testing.T) {
	suite.Run(t, new(stockLocationResponseTestSuite))
}

func (suite *stockLocationResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *stockLocationResponseTestSuite) Test_NewStockLocationFromModel() {
	id := uint(1)
	name, locationType := "First Location", "Warehouse"

	addressName := "WH Address"
	addressRegionID := uint(1)
	addressCity := "Moscow"
	addressZip := "ZIP-ZIP"
	addressAddress := "Nowhere st"
	addressPhoneNumber := "Don't call me"

	model := &models.StockLocation{
		Name: name,
		Type: locationType,
		Address: &models.Address{
			Name:        addressName,
			RegionID:    addressRegionID,
			City:        addressCity,
			Zip:         addressZip,
			Address1:    addressAddress,
			PhoneNumber: addressPhoneNumber,
		},
	}
	model.ID = id

	response := NewStockLocationFromModel(model)

	suite.assert.Equal(id, response.ID)
	suite.assert.Equal(name, response.Name)
	suite.assert.Equal(locationType, response.Type)
	suite.assert.Equal(addressName, response.Address.Name)
}

func (suite *stockLocationResponseTestSuite) Test_JSONEncoding() {
	id := uint(1)
	name, locationType := "First Location", "Warehouse"

	addressName := "WH Address"
	addressCity := "Moscow"
	addressZip := "ZIP-ZIP"
	addressAddress := "Nowhere st"
	addressPhoneNumber := "Don't call me"
	region := Region{uint(1), "Texas", uint(2), "USA"}

	response := &StockLocation{
		ID:   id,
		Name: name,
		Type: locationType,
		Address: &Address{
			ID:          id,
			Name:        addressName,
			Region:      region,
			City:        addressCity,
			Zip:         addressZip,
			Address1:    addressAddress,
			Address2:    addressAddress,
			PhoneNumber: addressPhoneNumber,
		},
	}
	expected := fmt.Sprintf(
		`{"id":%d,"name":"%s","type":"%s","address":{"id":%d,"name":"%s","region":{"id":%v,"name":"%v","countryId":%v,"countryName":"%v"},"city":"%s","zip":"%s","address1":"%s","address2":"%s","phoneNumber":"%s"}}`,
		id, name, locationType, id, addressName, region.ID, region.Name, region.CountryID, region.CountryName, addressCity, addressZip, addressAddress, addressAddress, addressPhoneNumber)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}

func (suite *stockLocationResponseTestSuite) Test_JSONEncoding_EmptyAddress() {
	id := uint(1)
	name, locationType := "First Location", "Warehouse"

	response := &StockLocation{ID: id, Name: name, Type: locationType}
	expected := fmt.Sprintf(`{"id":%d,"name":"%s","type":"%s"}`, id, name, locationType)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	err := encoder.Encode(response)
	actual := writer.String()

	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
