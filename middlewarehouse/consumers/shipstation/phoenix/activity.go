package phoenix

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

type Activity struct {
	Type string `json:"activity_type" binding:"required"`
	Data string `json:"data"`
}

func NewActivityFromAvro(message metamorphosis.AvroMessage) (*Activity, exceptions.IException) {
	a := new(Activity)
	err := json.Unmarshal(message.Bytes(), a)
	return a, activities.NewActivityException(err)
}
