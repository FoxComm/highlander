package errors

import (
	"strings"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
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

func (e *AggregateError) Error() string {
	return strings.Join(e.Messages(), ", ")
}

func (e *AggregateError) ToJsonStruct() []interface{} {
	var result []interface{}

	for _, err := range e.errors {
		if skuItemErr, ok := err.(*responses.InvalidSKUItemError); ok {
			result = append(result, skuItemErr)
		} else {
			result = append(result, err.Error())
		}
	}

	return result
}

func (e *AggregateError) Messages() []string {
	result := []string{}

	for _, err := range e.errors {
		result = append(result, err.Error())
	}

	return result
}
