package responses

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestCarrierResponseSuite(t *testing.T) {
	suite.Run(t, new(CarrierResponseTestSuite))
}

func (suite *CarrierResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *CarrierResponseTestSuite) Test_NewCarrierFromModel_ReturnsValidResponse() {
	//arrange
	id, name, trackingTemplate := uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	model := &models.Carrier{id, name, trackingTemplate}

	//act
	response := NewCarrierFromModel(model)

	//assert
	suite.assert.Equal(id, response.ID)
	suite.assert.Equal(name, response.Name)
	suite.assert.Equal(trackingTemplate, response.TrackingTemplate)
}

func (suite *CarrierResponseTestSuite) Test_CarrierEncoding_RunsNormally() {
	//arrange
	id, name, trackingTemplate := uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	response := &Carrier{id, name, trackingTemplate}
	expected := fmt.Sprintf(`{"id":%v,"name":"%v","trackingTemplate":"%v"}`, id, name, trackingTemplate)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
