package shared

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
)

type CustomerGroup struct {
	CustomerGroup *payloads.CustomerGroupPayload `json:"customerGroup" binding:"required"`
}

func NewCustomerGroupFromActivity(activity activities.ISiteActivity) (*payloads.CustomerGroupPayload, error) {
	bt := []byte(activity.Data())
	cg := new(CustomerGroup)
	err := json.Unmarshal(bt, cg)
	return cg.CustomerGroup, err
}
