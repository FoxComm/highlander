package payloads

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type stockLocationTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestStockLocationSuite(t *testing.T) {
	suite.Run(t, new(stockLocationTestSuite))
}

func (suite *stockLocationTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *stockLocationTestSuite) Test_StockLocation() {
	name, locationType := "First Location", "Warehouse"

	addressName := "WH Address"
	addressRegionID := 1
	addressCity := "Moscow"
	addressZip := "ZIP-ZIP"
	addressAddress := "Nowhere st"
	addressPhoneNumber := "Don't call me"

	raw := fmt.Sprintf(`{
		"name": "%s",
		"type": "%s",
		"address": {
			"name": "%s",
			"regionId": %d,
			"city": "%s",
			"zip": "%s",
			"address1": "%s",
			"phoneNumber": "%s"
		}
	}`, name, locationType, addressName, addressRegionID, addressCity, addressZip, addressAddress, addressPhoneNumber)

	decoder := json.NewDecoder(strings.NewReader(raw))

	var payload StockLocation
	err := decoder.Decode(&payload)

	suite.assert.Nil(err)
	suite.assert.Equal(name, payload.Name)
	suite.assert.Equal(locationType, payload.Type)
	suite.assert.Equal(addressName, payload.Address.Name)
}

func (suite *stockLocationTestSuite) Test_StockLocation_NoAddress() {
	name, locationType := "First Location", "Warehouse"

	raw := fmt.Sprintf(`{
		"name": "%s",
		"type": "%s"
	}`, name, locationType)

	decoder := json.NewDecoder(strings.NewReader(raw))

	var payload StockLocation
	err := decoder.Decode(&payload)

	suite.assert.Nil(err)
	suite.assert.Equal(name, payload.Name)
	suite.assert.Equal(locationType, payload.Type)
	suite.assert.Nil(payload.Address)
}
