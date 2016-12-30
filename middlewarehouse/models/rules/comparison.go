package rules

import "fmt"

// Comparison is a function definition for comparing two booleans.
type Comparison func(bool, bool) bool

// NewComparison creates a Comparison based on a type that is passed in.
func NewComparison(comparisonType string) (Comparison, error) {
	switch comparisonType {
	case And:
		return func(l, r bool) bool { return l && r }, nil
	case Or:
		return func(l, r bool) bool { return l || r }, nil
	}

	return nil, fmt.Errorf(errorCantCreateComparison, comparisonType)
}
