package errors

import (
	"bytes"
	"encoding/json"
)

type AggregateError struct {
	errors []error
}

func (e *AggregateError) Add(err error) {
	e.errors = append(e.errors, err)
}

func (e AggregateError) Error() string {
	buffer := new(bytes.Buffer)

	json.NewEncoder(buffer).Encode(e.ToJSON())

	return buffer.String()
}

func (e *AggregateError) ToJSON() []string {
	result := []string{}

	for _, err := range e.errors {
		result = append(result, err.Error())
	}

	return result
}
