package responses

import "encoding/json"

type Error struct {
	Errors []interface{} `json:"errors"`
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
