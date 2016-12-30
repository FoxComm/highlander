package rules

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type RuleTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestRuleSuite(t *testing.T) {
	suite.Run(t, new(RuleTestSuite))
}

func (suite *RuleTestSuite) Test_JsonDeserialization_Success() {
	jsonRule := []byte(`
	{
		"comparison": "and",
		"conditions": [
			{
				"rootObject": "ShippingAddress",
				"field": "address1",
				"operator": "notContains",
				"value": "p.o. box"
			},
			{
				"rootObject": "ShippingAddress",
				"field": "countryId",
				"operator": "equals",
				"value": 1
			}
		]
	}`)

	rule := new(Rule)
	err := json.Unmarshal(jsonRule, rule)
	suite.Nil(err)

	suite.Equal(And, rule.Comparison)
	suite.Len(rule.Conditions, 2)

	condition1 := rule.Conditions[0]
	suite.Equal("ShippingAddress", condition1.RootObject)
	suite.Equal("address1", condition1.Field)
	suite.Equal(NotContains, condition1.Operator)
	suite.Equal("p.o. box", condition1.Value.(string))

	condition2 := rule.Conditions[1]
	suite.Equal("ShippingAddress", condition2.RootObject)
	suite.Equal("countryId", condition2.Field)
	suite.Equal(Equals, condition2.Operator)
	suite.Equal(1, int(condition2.Value.(float64)))
}
