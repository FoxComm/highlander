package errors

import (
	"strings"
)

type AggregateError struct {
	errors []error
}

func (e *AggregateError) Add(err error) {
	e.errors = append(e.errors, err)
}

func (e *AggregateError) Length() int {
	return len(e.errors)
}

func (e AggregateError) Error() string {
	return strings.Join(e.Messages(), ", ")
}

func (e *AggregateError) Messages() []string {
	result := []string{}

	for _, err := range e.errors {
		result = append(result, err.Error())
	}

	return result
}
