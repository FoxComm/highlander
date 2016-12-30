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

	valInt, err := toInt(c.Value)
	if err != nil {
		return false, err
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

	valInt, err := toUint(c.Value)
	if err != nil {
		return false, err
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

func toInt(value interface{}) (int, error) {
	// If the condition has been written to JSON the value will be a float64
	// rather than an int. First, try to convert to int, if that fails, fall back
	// to float64.
	valInt, ok := value.(int)
	if !ok {
		valFloat, ok := value.(float64)
		if !ok {
			return 0, fmt.Errorf(errorInvalidTypeCast, "integer")
		}
		valInt = int(valFloat)
	}
	return valInt, nil
}

func toUint(value interface{}) (uint, error) {
	// If the condition has been written to JSON the value will be a float64
	// rather than an unsigned int. First, try to convert to int, if that fails,
	// fall back to float64.
	valInt, ok := value.(uint)
	if !ok {
		valFloat, ok := value.(float64)
		if !ok {
			return 0, fmt.Errorf(errorInvalidTypeCast, "unsigned integer")
		}
		valInt = uint(valFloat)
	}
	return valInt, nil
}
