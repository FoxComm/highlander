package rules

import (
	"fmt"
	"strings"
)

type Condition struct {
	RootObject string      `json:"rootObject"`
	Field      string      `json:"field"`
	Operator   string      `json:"operator"`
	Value      interface{} `json:"value"`
}

func (c Condition) MatchesBool(comp bool) (bool, error) {
	var result bool
	var err error

	valBool, ok := c.Value.(bool)
	if !ok {
		return false, fmt.Errorf(errorInvalidTypeCast, "boolean")
	}

	switch c.Operator {
	case Equals:
		result = comp == valBool
	case NotEquals:
		result = comp != valBool
	default:
		err = fmt.Errorf(errorInvalidComparison, c.Operator, "boolean")
	}

	return result, err
}

func (c Condition) MatchesInt(comp int) (bool, error) {
	var result bool
	var err error

	valInt, ok := c.Value.(int)
	if !ok {
		return false, fmt.Errorf(errorInvalidTypeCast, "integer")
	}

	switch c.Operator {
	case Equals:
		result = comp == valInt
	case NotEquals:
		result = comp != valInt
	case GreaterThan:
		result = comp > valInt
	case GreaterThanOrEquals:
		result = comp >= valInt
	case LessThan:
		result = comp < valInt
	case LessThanOrEquals:
		result = comp <= valInt
	default:
		err = fmt.Errorf(errorInvalidComparison, c.Operator, "boolean")
	}

	return result, err
}

func (c Condition) MatchesString(comp string) (bool, error) {
	var result bool
	var err error

	valStr, ok := c.Value.(string)
	if !ok {
		return false, fmt.Errorf(errorInvalidTypeCast, "string")
	}

	switch c.Operator {
	case Equals:
		result = comp == valStr
	case NotEquals:
		result = comp != valStr
	case Contains:
		result = strings.Contains(comp, valStr)
	case NotContains:
		result = !strings.Contains(comp, valStr)
	case StartsWith:
		result = strings.HasPrefix(comp, valStr)
	case InArray:
		result = isInArray(comp, valStr)
	case NotInArray:
		result = !isInArray(comp, valStr)
	default:
		err = fmt.Errorf(errorInvalidComparison, c.Operator, "string")
	}

	return result, err
}

func (c Condition) MatchesUint(comp uint) (bool, error) {
	var result bool
	var err error

	valInt, ok := c.Value.(uint)
	if !ok {
		return false, fmt.Errorf(errorInvalidTypeCast, "unsigned integer")
	}
	switch c.Operator {
	case Equals:
		result = comp == valInt
	case NotEquals:
		result = comp != valInt
	case GreaterThan:
		result = comp > valInt
	case GreaterThanOrEquals:
		result = comp >= valInt
	case LessThan:
		result = comp < valInt
	case LessThanOrEquals:
		result = comp <= valInt
	default:
		err = fmt.Errorf(errorInvalidComparison, c.Operator, "boolean")
	}

	return result, err
}

func isInArray(comp string, val string) bool {
	valArray := strings.Split(val, ", ")

	for _, v := range valArray {
		if comp == v {
			return true
		}
	}

	return false
}
