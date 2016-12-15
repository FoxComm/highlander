package errors

import (
	"errors"
	"fmt"
	"strings"
)

const (
	mustBeGreaterThan = "%s.%s must be greater than %d - actual is %d"
	mustNotBeEmpty    = "%s.%s must not be empty"
	when              = "%s when %s"
)

type Validation struct {
	err   AggregateError
	model string
}

func NewValidation(model string) *Validation {
	err := AggregateError{}
	return &Validation{err, model}
}

func (v *Validation) Error() error {
	if v.err.Length() > 0 {
		return v.err
	}

	return nil
}

func (v *Validation) NonEmpty(value string, field string, messages ...string) {
	if value != "" {
		return
	}

	rawMessage := fmt.Sprintf(
		mustNotBeEmpty,
		strings.ToLower(v.model),
		strings.ToLower(field),
	)

	v.err.Add(createWhenError(rawMessage, messages...))
}

func (v *Validation) GreaterThanF(expected float64, actual float64, field string, messages ...string) {
	if actual > expected {
		return
	}

	rawMessage := fmt.Sprintf(
		mustBeGreaterThan,
		strings.ToLower(v.model),
		strings.ToLower(field),
		expected,
		actual,
	)

	v.err.Add(createWhenError(rawMessage, messages...))
}

func createWhenError(message string, whenMessages ...string) error {
	if len(whenMessages) == 0 {
		return errors.New(message)
	}

	return fmt.Errorf(
		when,
		message,
		strings.Join(whenMessages, ", "),
	)
}
