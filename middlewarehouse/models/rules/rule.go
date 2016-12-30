package rules

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
)

// Rule represents a structured set of conditions and nested rules that can be
// evaluated against an object.
type Rule struct {
	Comparison string      `json:"comparison"`
	Conditions []Condition `json:"conditions"`
	Rules      []Rule      `json:"statements"` // Keeping the JSON labeled 'statements' for legacy purposes.
}

// IsEmpty returns true if the rule has no parameters.
func (r Rule) IsEmpty() bool {
	return r.Comparison == "" && len(r.Conditions) == 0 && len(r.Rules) == 0
}

// Evaluate determines whether a piece of data passes or fails validation
// against this rule, given some specific RuleEvaluator.
func (r *Rule) Evaluate(data interface{}, f RuleEvaluator) (bool, error) {
	result := r.Comparison == And
	comparer, err := NewComparison(r.Comparison)
	if err != nil {
		return false, nil
	}

	for _, condition := range r.Conditions {
		condResult, err := f(condition, data)
		if err != nil {
			return false, err
		}

		result = comparer(result, condResult)
	}

	for _, rule := range r.Rules {
		ruleResult, err := rule.Evaluate(data, f)
		if err != nil {
			return false, err
		}

		result = comparer(result, ruleResult)
	}

	return result, nil
}

// Scan is an interface used for getting JSON out of the database
// and it turns it into a struct.
func (r *Rule) Scan(src interface{}) error {
	source, ok := src.([]byte)
	if !ok {
		return errors.New("Type assertion .([]byte) failed.")
	}

	return json.Unmarshal(source, r)
}

// Value converts the struct to a byte array of JSON.
func (r Rule) Value() (driver.Value, error) {
	j, err := json.Marshal(r)
	return j, err
}
