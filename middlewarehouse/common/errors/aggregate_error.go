package errors

import (
	"strings"
)

type AggregateError struct {
	Errors []error
}

func (e *AggregateError) Add(err error) {
	e.Errors = append(e.Errors, err)
}

func (e *AggregateError) Length() int {
	return len(e.Errors)
}

func (e *AggregateError) Error() string {
	return strings.Join(e.Messages(), ", ")
}

func (e *AggregateError) Messages() []string {
	result := []string{}

	for _, err := range e.Errors {
		result = append(result, err.Error())
	}

	return result
}
