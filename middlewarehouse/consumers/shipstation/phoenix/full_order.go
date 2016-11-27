package phoenix

import (
	"encoding/json"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

type FullOrder struct {
	Order Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity *Activity) (*FullOrder, exceptions.IException) {
	bt := []byte(activity.Data)
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, activities.NewActivityException(err)
}
