package models

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierModelTestSuite struct {
	suite.Suite
}

func TestCarrierModelSuite(t *testing.T) {
	suite.Run(t, new(suite.Suite))
}

func (suite *CarrierModelTestSuite) TestNewCarrierFromPayload() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	payload := &payloads.Carrier{name, trackingTemplate}

	//act
	model := NewCarrierFromPayload(payload)

	//assert
	assert.Equal(suite.T(), name, model.Name)
	assert.Equal(suite.T(), trackingTemplate, model.TrackingTemplate)
}
