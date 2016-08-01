package responses

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/gormfox"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShipmentLineItemResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShipmentLineItemResponseSuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemResponseTestSuite))
}

func (suite *ShipmentLineItemResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentLineItemResponseTestSuite) Test_NewShipmentLineItemFromModel_ReturnsValidResponse() {
	//arrange
	id := uint(1)
	shipmentID := uint(2)
	referenceNumber := "LI0001"
	sku := "TEST-SKU"
	name := "Test SKU"
	price := uint(4999)
	imagePath := "http://test.com/test.png"
	state := "pending"

	model := &models.ShipmentLineItem{gormfox.Base{}, shipmentID, referenceNumber, sku, name, price, imagePath, state}
	model.ID = id

	//act
	response := NewShipmentLineItemFromModel(model)

	//assert
	suite.assert.Equal(id, response.ID)
	suite.assert.Equal(referenceNumber, response.ReferenceNumber)
	suite.assert.Equal(sku, response.SKU)
	suite.assert.Equal(name, response.Name)
	suite.assert.Equal(price, response.Price)
	suite.assert.Equal(imagePath, response.ImagePath)
	suite.assert.Equal(state, response.State)
}

func (suite *ShipmentLineItemResponseTestSuite) Test_ShipmentLineItemEncoding_RunsNormally() {
	//arrange
	id := uint(1)
	referenceNumber := "LI0001"
	sku := "TEST-SKU"
	name := "Test SKU"
	price := uint(4999)
	imagePath := "http://test.com/test.png"
	state := "pending"

	response := &ShipmentLineItem{id, referenceNumber, sku, name, price, imagePath, state}
	expected := fmt.Sprintf(
		`{"id":%v,"referenceNumber":"%v","sku":"%v","name":"%v","price":%v,"imagePath":"%v","state":"%v"}`,
		id, referenceNumber, sku, name, price, imagePath, state)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
