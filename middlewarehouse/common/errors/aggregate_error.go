package errors

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
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

func (e *AggregateError) Error() string {
	return strings.Join(e.Messages(), ", ")
}

func (e *AggregateError) ToReservationError() responses.ReservationError {
	var errArray []responses.InvalidSKUItemError

	for _, err := range e.errors {
		skuItemErr := responses.NewInvalidSKUItemError(err)
		errArray = append(errArray, skuItemErr)
	}

	return responses.ReservationError{Errors: errArray}
}

func (e *AggregateError) Messages() []string {
	result := []string{}

	for _, err := range e.errors {
		result = append(result, err.Error())
	}

	return result
}
