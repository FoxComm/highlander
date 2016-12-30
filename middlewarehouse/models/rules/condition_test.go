package rules

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ConditionTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}

func TestConditionSuite(t *testing.T) {
	suite.Run(t, new(ConditionTestSuite))
}

func (suite *ConditionTestSuite) Test_BoolMatchesTrue_Success() {
	suite.testBool(Equals, true, true, true)
}

func (suite *ConditionTestSuite) Test_BoolMatchesFalse_Success() {
	suite.testBool(Equals, false, false, true)
}

func (suite *ConditionTestSuite) Test_BoolMatchesTrue_Failure() {
	suite.testBool(Equals, false, true, false)
}

func (suite *ConditionTestSuite) Test_BoolMatchesFalse_False() {
	suite.testBool(Equals, true, false, false)
}

func (suite *ConditionTestSuite) testBool(operator string, condValue, compValue, expected bool) {
	condition := Condition{Operator: operator, Value: condValue}
	res, err := condition.MatchesBool(compValue)
	suite.Nil(err)
	suite.Equal(expected, res)
}

func (suite *ConditionTestSuite) Test_BoolInvalidType_Failure() {
	condition := Condition{Operator: Equals, Value: "a string"}
	_, err := condition.MatchesBool(false)
	suite.NotNil(err)
	suite.Equal(fmt.Errorf(errorInvalidTypeCast, "boolean"), err)
}

func (suite *ConditionTestSuite) Test_BoolInvalidOperator_Failure() {
	condition := Condition{Operator: Contains, Value: true}
	_, err := condition.MatchesBool(true)
	suite.NotNil(err)
	suite.Equal(fmt.Errorf(errorInvalidComparison, Contains, "boolean"), err)
}

func (suite *ConditionTestSuite) Test_IntEquals_TrueWhenEqual() {
	suite.testInt(Equals, 50, 50, true)
}

func (suite *ConditionTestSuite) Test_IntEquals_FalseWhenNotEqual() {
	suite.testInt(Equals, 50, 51, false)
}

func (suite *ConditionTestSuite) Test_IntNotEquals_TrueWhenNotEqual() {
	suite.testInt(NotEquals, 50, 51, true)
}

func (suite *ConditionTestSuite) Test_IntNotEquals_FalseWhenEqual() {
	suite.testInt(NotEquals, 50, 50, false)
}

func (suite *ConditionTestSuite) Test_IntGreaterThan_TrueWhenGreater() {
	suite.testInt(GreaterThan, 50, 51, true)
}

func (suite *ConditionTestSuite) Test_IntGreaterThan_FalseWhenEqual() {
	suite.testInt(GreaterThan, 50, 50, false)
}

func (suite *ConditionTestSuite) Test_IntGreaterThan_FalseWhenLess() {
	suite.testInt(GreaterThanOrEquals, 50, 49, false)
}

func (suite *ConditionTestSuite) Test_IntGreaterThanOrEquals_TrueWhenGreater() {
	suite.testInt(GreaterThanOrEquals, 50, 51, true)
}

func (suite *ConditionTestSuite) Test_IntGreaterThanOrEquals_TrueWhenEqual() {
	suite.testInt(GreaterThanOrEquals, 50, 50, true)
}

func (suite *ConditionTestSuite) Test_IntGreaterThanOrEquals_FalseWhenLess() {
	suite.testInt(GreaterThanOrEquals, 50, 49, false)
}

func (suite *ConditionTestSuite) Test_IntLessThan_FalseWhenGreater() {
	suite.testInt(LessThan, 50, 51, false)
}

func (suite *ConditionTestSuite) Test_IntLessThan_FalseWhenEqual() {
	suite.testInt(LessThan, 50, 50, false)
}

func (suite *ConditionTestSuite) Test_IntLessThan_TrueWhenLess() {
	suite.testInt(LessThan, 50, 49, true)
}

func (suite *ConditionTestSuite) Test_IntLessThanOrEquals_FalseWhenGreater() {
	suite.testInt(LessThanOrEquals, 50, 51, false)
}

func (suite *ConditionTestSuite) Test_IntLessThanOrEquals_TrueWhenEqual() {
	suite.testInt(LessThanOrEquals, 50, 50, true)
}

func (suite *ConditionTestSuite) Test_IntLessThanOrEquals_TrueWhenLess() {
	suite.testInt(LessThanOrEquals, 50, 49, true)
}

func (suite *ConditionTestSuite) testInt(operator string, condValue, compValue int, expected bool) {
	condition := Condition{Operator: operator, Value: condValue}
	res, err := condition.MatchesInt(compValue)
	suite.Nil(err)
	suite.Equal(expected, res)
}

func (suite *ConditionTestSuite) Test_StringEquals_TrueWhenEqual() {
	suite.testString(Equals, "some string", "some string", true)
}

func (suite *ConditionTestSuite) Test_StringEquals_FalseWhenNotEqual() {
	suite.testString(Equals, "some string", "another string", false)
}

func (suite *ConditionTestSuite) Test_StringNotEquals_FalseWhenEqual() {
	suite.testString(NotEquals, "some string", "some string", false)
}

func (suite *ConditionTestSuite) Test_StringNotEquals_TrueWhenNotEqual() {
	suite.testString(NotEquals, "some string", "another string", true)
}

func (suite *ConditionTestSuite) Test_StringContains_TrueWhenContains() {
	suite.testString(Contains, "me str", "some string", true)
}

func (suite *ConditionTestSuite) Test_StringContains_FalseWhenNotContains() {
	suite.testString(Contains, "me str", "another string", false)
}

func (suite *ConditionTestSuite) Test_StringNotContains_FalseWhenContains() {
	suite.testString(NotContains, "me str", "some string", false)
}

func (suite *ConditionTestSuite) Test_StringNotContains_TrueWhenNotContains() {
	suite.testString(NotContains, "me str", "another string", true)
}

func (suite *ConditionTestSuite) testString(operator, condValue, compValue string, expected bool) {
	condition := Condition{Operator: operator, Value: condValue}
	res, err := condition.MatchesString(compValue)
	suite.Nil(err)
	suite.Equal(expected, res)
}
