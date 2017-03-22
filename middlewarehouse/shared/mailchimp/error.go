package mailchimp

import (
	"encoding/json"
	"fmt"
)

type APIError struct {
	Status string `json:"status"`
	Code   int    `json:"code"`
	Name   string `json:"name"`
	Err    string `json:"error"`
}

func (e APIError) Error() string {
	return fmt.Sprintf("%v: %v", e.Code, e.Err)
}

func errorCheck(body []byte) error {
	var e APIError
	json.Unmarshal(body, &e)
	if e.Err != "" || e.Code != 0 {
		return e
	}

	return nil
}
