package rules

// Comparison types.
const (
	And = "and"
	Or  = "or"

	Equals              = "equals"
	NotEquals           = "notEquals"
	GreaterThan         = "greaterThan"
	GreaterThanOrEquals = "greaterThanOrEquals"
	LessThan            = "lessThan"
	LessThanOrEquals    = "lessThanOrEquals"
	Contains            = "contains"
	NotContains         = "notContains"
	StartsWith          = "startsWith"
	InArray             = "inArray"
	NotInArray          = "notInArray"
)

// Error messages.
const (
	errorCantCreateComparison = "Unable to create comparison for type '%s'"
	errorInvalidComparison    = "Invalid operator for '%s' for %s comparison"
	errorInvalidTypeCast      = "Error in %s comparison -- type of value is invalid"
	invalidQueryComparison    = "Invalid query comparison %s"
)
