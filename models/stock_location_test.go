package models

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type stockLocationModelTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestStockLocationModelSuite(t *testing.T) {
	suite.Run(t, new(stockLocationModelTestSuite))
}

func (suite *stockLocationModelTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *stockLocationModelTestSuite) Test_NewStockLocationFromPayload() {
	name, locationType := "First Location", "Warehouse"

	addressName := "WH Address"
	addressRegionID := uint(1)
	addressCity := "Moscow"
	addressZip := "ZIP-ZIP"
	addressAddress := "Nowhere st"
	addressPhoneNumber := "Don't call me"

	payload := &payloads.StockLocation{name, locationType, &payloads.Address{
		Name:        addressName,
		RegionID:    addressRegionID,
		City:        addressCity,
		Zip:         addressZip,
		Address1:    addressAddress,
		PhoneNumber: addressPhoneNumber,
	}}

	model := NewStockLocationFromPayload(payload)

	println("Test_NewStockLocationFromPayload")

	suite.assert.Equal(name, model.Name)
	suite.assert.Equal(locationType, model.Type)
	suite.assert.Equal(addressName, model.Address.Name)
}

func (suite *stockLocationModelTestSuite) Test_NewStockLocationFromPayload_EmptyAddress() {
	name, locationType := "First Location", "Warehouse"

	payload := &payloads.StockLocation{Name: name, Type: locationType}

	model := NewStockLocationFromPayload(payload)

	suite.assert.Equal(name, model.Name)
	suite.assert.Equal(locationType, model.Type)
	suite.assert.Nil(model.Address)
}
