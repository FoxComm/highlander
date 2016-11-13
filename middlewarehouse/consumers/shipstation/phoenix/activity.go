package phoenix

import (
	"encoding/json"

	"github.com/FoxComm/metamorphosis"
)

type Activity struct {
	Type string `json:"activity_type" binding:"required"`
	Data string `json:"data"`
}

func NewActivityFromAvro(message metamorphosis.AvroMessage) (*Activity, error) {
	a := new(Activity)
	err := json.Unmarshal(message.Bytes(), a)
	return a, err
}
