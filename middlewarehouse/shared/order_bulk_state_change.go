package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

type OrderBulkStateChange struct {
	CordRefNums []string `json:"cordRefNums" binding:"required"`
	NewState    string   `json:"newState" binding:"required"`
}

func NewOrderBulkStateChangeFromActivity(activity activities.ISiteActivity) (*OrderBulkStateChange, error) {
	bt := []byte(activity.Data())
	fo := new(OrderBulkStateChange)
	err := json.Unmarshal(bt, fo)
	return fo, err
}
