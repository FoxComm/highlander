package responses

import (
	"encoding/json"
)

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

func NewInvalidSKUItemError(err error) InvalidSKUItemError {
	if skuErr, ok := err.(*InvalidSKUItemError); ok {
		return *skuErr
	}
	return InvalidSKUItemError{Sku: "", Debug: err.Error()}
}

func NewReservationError(err error) ReservationError {
	return ReservationError{
		Errors: []InvalidSKUItemError{
			NewInvalidSKUItemError(err),
		},
	}
}
