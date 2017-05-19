package errors

import (
	"errors"
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

func (e *AggregateError) ToReservationError() (*responses.ReservationError, error) {
	var errArray []responses.InvalidSKUItemError

	for _, err := range e.errors {
		if skuErr, ok := err.(*responses.InvalidSKUItemError); ok {
			errArray = append(errArray, *skuErr)
		} else {
			return nil, errors.New("Not all errors are related to invalid SKU")
		}
	}

	return &responses.ReservationError{Errors: errArray}, nil
}

func (e *AggregateError) Messages() []string {
	result := []string{}

	for _, err := range e.errors {
		result = append(result, err.Error())
	}

	return result
}
