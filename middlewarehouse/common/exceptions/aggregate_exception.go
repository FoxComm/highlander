package exceptions

import (
	"strings"
)

type AggregateException struct {
	exceptions []IException
}

func (ex AggregateException) Add(exception IException) {
	ex.exceptions = append(ex.exceptions, exception)
}

func (ex AggregateException) Length() int {
	return len(ex.exceptions)
}

func (ex AggregateException) ToString() string {
	result := []string{}

	for _, exception := range ex.exceptions {
		result = append(result, exception.ToString())
	}

	return strings.Join(result, ", ")
}

func (ex AggregateException) ToJSON() interface{} {
	result := []interface{}{}

	for _, exception := range ex.exceptions {
		result = append(result, exception.ToJSON())
	}

	return result
}
