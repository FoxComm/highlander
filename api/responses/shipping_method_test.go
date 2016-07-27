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

type ShippingMethodResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestShippingMethodResponseSuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodResponseTestSuite))
}

func (suite *ShippingMethodResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodResponseTestSuite) Test_NewShippingMethodFromModel_ReturnsValidResponse() {
	//arrange
	id, carrierID, name := uint(1), uint(2), "UPS 2 days ground"
	model := &models.ShippingMethod{id, carrierID, name}

	//act
	response := NewShippingMethodFromModel(model)

	//assert
	suite.assert.Equal(id, response.ID)
	suite.assert.Equal(carrierID, response.CarrierID)
	suite.assert.Equal(name, response.Name)
}

func (suite *ShippingMethodResponseTestSuite) Test_ShippingMethodEncoding_RunsNormally() {
	//arrange
	id, carrierID, name := uint(1), uint(2), "UPS 2 days ground"
	response := &ShippingMethod{id, carrierID, name}
	expected := fmt.Sprintf(`{"id":%v,"carrierId":%v,"name":"%v"}`, id, carrierID, name)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
