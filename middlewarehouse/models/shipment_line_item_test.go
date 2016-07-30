package models

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShipmentLineItemModelTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShipmentLineItemModelSuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemModelTestSuite))
}

func (suite *ShipmentLineItemModelTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentLineItemModelTestSuite) Test_NewShipmentLineItemFromPayload_ReturnsValidModel() {
	//arrange
	referenceNumber := "LI0001"
	sku := "TEST-SKU"
	name := "Test SKU"
	price := uint(4999)
	imagePath := "http://test.com/test.png"
	state := "pending"

	payload := &payloads.ShipmentLineItem{referenceNumber, sku, name, price, imagePath, state}

	//act
	model := NewShipmentLineItemFromPayload(payload)

	//assert
	suite.assert.Equal(referenceNumber, model.ReferenceNumber)
	suite.assert.Equal(sku, model.SKU)
	suite.assert.Equal(name, model.Name)
	suite.assert.Equal(price, model.Price)
	suite.assert.Equal(imagePath, model.ImagePath)
	suite.assert.Equal(state, model.State)
}
