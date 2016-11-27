package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity activities.ISiteActivity) (*FullOrder, exceptions.IException) {
	bt := []byte(activity.Data())
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, activities.NewActivityException(err)
}
