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
}

func TestCarrierPayloadSuite(t *testing.T) {
	suite.Run(t, new(CarrierPayloadTestSuite))
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
	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), name, payload.Name)
	assert.Equal(suite.T(), trackingTemplate, payload.TrackingTemplate)
}
