package responses

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierResponseTestSuite struct {
	suite.Suite
}

func TestCarrierResponseSuite(t *testing.T) {
	suite.Run(t, new(suite.Suite))
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
