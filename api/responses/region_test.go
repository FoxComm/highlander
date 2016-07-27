package responses

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type RegionResponseTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestRegionResponseSuite(t *testing.T) {
	suite.Run(t, new(RegionResponseTestSuite))
}

func (suite *RegionResponseTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
}

func (suite *RegionResponseTestSuite) Test_RegionEncoding_RunsNormally() {
	//arrange
	id, name, countryID, countryName := uint(1), "Texas", uint(2), "USA"
	response := &Region{id, name, countryID, countryName}
	expected := fmt.Sprintf(`{"id":%v,"name":"%v","countryId":%v,"countryName":"%v"}`, id, name, countryID, countryName)
	writer := new(bytes.Buffer)
	encoder := json.NewEncoder(writer)

	//act
	err := encoder.Encode(response)
	actual := writer.String()

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(expected, strings.TrimSpace(actual))
}
