package models

import (
	"fmt"
	"strings"
)

const (
	Equals              = "Equals"
	NotEquals           = "NotEquals"
	GreaterThan         = "GreaterThan"
	GreaterThanOrEquals = "GreaterThanOrEquals"
	LessThan            = "LessThan"
	LessThanOrEquals    = "LessThanOrEquals"
	Contains            = "Contains"
	NotContains         = "NotContains"
	StartsWith          = "StartsWith"
	InArray             = "InArray"
	NotInArray          = "NotInArray"

	errorInvalidComparison = "Invalid operator for '%s' for %s comparison"
	errorInvalidTypeCast   = "Error in %s comparison -- type of value is invalid"
)

type Condition struct {
	RootObject string      `json:"rootObject"`
	Field      string      `json:"field"`
	Operator   string      `json:"operator"`
	Value      interface{} `json:"value"`
}

func (c Condition) MatchesBool(comp bool) (result bool, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf(errorInvalidTypeCast, "boolean")
		}
	}()

	valBool := c.Value.(bool)
	switch c.Operator {
	case Equals:
		result = comp == valBool
	case NotEquals:
		result = comp != valBool
	default:
		err = fmt.Errorf(errorInvalidComparison, c.Operator, "boolean")
	}

	return
}

func (c Condition) MatchesInt(comp int) (result bool, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf(errorInvalidTypeCast, "integer")
		}
	}()

	valInt := c.Value.(int)
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

	return
}

func (c Condition) MatchesString(comp string) (result bool, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf(errorInvalidTypeCast, "string")
		}
	}()

	valStr := c.Value.(string)
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

	return
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
