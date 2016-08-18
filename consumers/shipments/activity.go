package shipments

import (
	"encoding/json"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/common/lib/phoenix"
)

func NewActivityFromAvro(message metamorphosis.AvroMessage) (*phoenix.Activity, error) {
	a := new(phoenix.Activity)
	err := json.Unmarshal(message.Bytes(), a)
	return a, err
}
