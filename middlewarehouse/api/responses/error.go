package responses

import (
	"encoding/json"
)

type ErrorResponse interface {
	GetAllErrors() []string
}

type Error struct {
	Errors []string `json:"errors"`
}

type ReservationError struct {
	Errors []InvalidSKUItemError `json:"errors"`
}

type InvalidSKUItemError struct {
	Sku   string `json:"sku"`
	Debug string `json:"debug"`
}

func (err *InvalidSKUItemError) Error() string {
	if result, err := json.Marshal(err); err != nil {
		return err.Error()
	} else {
		return string(result)
	}
}

func (err Error) GetAllErrors() []string {
	return err.Errors
}

func (err ReservationError) GetAllErrors() []string {
	var result []string
	for _, err := range err.Errors {
		result = append(result, err.Error())
	}
	return result
}
