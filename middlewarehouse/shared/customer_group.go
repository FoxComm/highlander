package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
)

type CustomerGroup struct {
	CustomerGroup *responses.CustomerGroupResponse `json:"customerGroup" binding:"required"`
}

func NewCustomerGroupFromActivity(activity activities.ISiteActivity) (*responses.CustomerGroupResponse, error) {
	bt := []byte(activity.Data())
	cg := new(CustomerGroup)
	err := json.Unmarshal(bt, cg)
	return cg.CustomerGroup, err
}
