package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierPayloadTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestCarrierPayloadSuite(t *testing.T) {
	suite.Run(t, new(CarrierPayloadTestSuite))
}

func (suite *CarrierPayloadTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *CarrierPayloadTestSuite) Test_CarrierDecoding_RunsNormally() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	raw := fmt.Sprintf(`{
		"name": "%v",
		"trackingTemplate": "%v"
	}`, name, trackingTemplate)
	decoder := json.NewDecoder(strings.NewReader(raw))

	//act
	var payload Carrier
	err := decoder.Decode(&payload)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(name, payload.Name)
	suite.assert.Equal(trackingTemplate, payload.TrackingTemplate)
}
