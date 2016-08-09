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
	carrier := models.Carrier{uint(2), "DHL", "https://dhl.com/tracking"}
	model := &models.ShippingMethod{uint(1), carrier.ID, carrier, "UPS 2 days ground"}

	//act
	response := NewShippingMethodFromModel(model)

	//assert
	suite.assert.Equal(model.ID, response.ID)
	suite.assert.Equal(model.Name, response.Name)
	suite.assert.Equal(carrier.ID, response.Carrier.ID)
}

func (suite *ShippingMethodResponseTestSuite) Test_ShippingMethodEncoding_RunsNormally() {
	//arrange
	carrier := Carrier{uint(2), "DHL", "https://dhl.com/tracking"}
	response := &ShippingMethod{uint(1), carrier, "UPS 2 days ground"}
	expected := fmt.Sprintf(`{"id":%v,"carrier":{"id":%v,"name":"%v","trackingTemplate":"%v"},"name":"%v"}`,
		response.ID, carrier.ID, carrier.Name, carrier.TrackingTemplate, response.Name)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
