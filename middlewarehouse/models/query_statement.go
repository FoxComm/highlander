package models

import "fmt"

const (
	And = "and"
	Or  = "or"

	invalidQueryComparison = "Invalid query comparison %s"
)

type QueryEvaluator func(Condition, interface{}) (bool, error)

type QueryStatement struct {
	Comparison string           `json:"comparison"`
	Conditions []Condition      `json:"conditions"`
	Statements []QueryStatement `json:"statements"`
}

type compareHelper func(bool, bool) bool

func andHelper(left, right bool) bool {
	return left && right
}

func orHelper(left, right bool) bool {
	return left || right
}

func (q *QueryStatement) Evaluate(data interface{}, f QueryEvaluator) (bool, error) {
	var comparer compareHelper
	result := q.Comparison == And

	switch q.Comparison {
	case And:
		comparer = andHelper
	case Or:
		comparer = orHelper
	default:
		return false, fmt.Errorf(invalidQueryComparison, q.Comparison)
	}

	for _, condition := range q.Conditions {
		condResult, err := f(condition, data)
		if err != nil {
			return false, err
		}

		result = comparer(result, condResult)
	}

	for _, statement := range q.Statements {
		stmtResult, err := statement.Evaluate(data, f)
		if err != nil {
			return false, err
		}

		result = comparer(result, stmtResult)
	}

	return result, nil
}
