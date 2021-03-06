package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

type FullOrder struct {
	Order payloads.Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity activities.ISiteActivity) (*FullOrder, error) {
	bt := []byte(activity.Data())
	fo := new(FullOrder)
	if err := json.Unmarshal(bt, fo); err != nil {
		return nil, err
	}

	fo.Order.SetScope(activity.Scope())
	return fo, nil
}

func NewFullOrderFromPayload(payload *payloads.OrderResult) *FullOrder {
	fo := new(FullOrder)
	fo.Order = payload.Order
	return fo
}
