package rules

// RuleEvaluator is a function definition for evaluating whether a Condition
// applies to an object.
type RuleEvaluator func(Condition, interface{}) (bool, error)
