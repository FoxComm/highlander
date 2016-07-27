package models

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierModelTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestCarrierModelSuite(t *testing.T) {
	suite.Run(t, new(CarrierModelTestSuite))
}

func (suite *CarrierModelTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *CarrierModelTestSuite) TestNewCarrierFromPayload() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	payload := &payloads.Carrier{name, trackingTemplate}

	//act
	model := NewCarrierFromPayload(payload)

	//assert
	suite.assert.Equal(name, model.Name)
	suite.assert.Equal(trackingTemplate, model.TrackingTemplate)
}
