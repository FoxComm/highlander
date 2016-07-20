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

type CarrierResponseTestSuite struct {
	suite.Suite
}

func TestCarrierResponseSuite(t *testing.T) {
	suite.Run(t, new(CarrierResponseTestSuite))
}

func (suite *CarrierResponseTestSuite) TestNewCarrierFromModel() {
	//arrange
	id, name, trackingTemplate := uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	model := &models.Carrier{id, name, trackingTemplate}

	//act
	response := NewCarrierFromModel(model)

	//assert
	assert.Equal(suite.T(), id, response.ID)
	assert.Equal(suite.T(), name, response.Name)
	assert.Equal(suite.T(), trackingTemplate, response.TrackingTemplate)
}

func (suite *CarrierResponseTestSuite) TestCarrierEncoding() {
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
	assert.Nil(suite.T(), err)
	fmt.Println(expected, actual)
	assert.Equal(suite.T(), expected, strings.TrimSpace(actual))
}
