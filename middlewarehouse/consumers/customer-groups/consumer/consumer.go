package consumer

import (
	"log"

	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityCustomerGroupCreated = "customer_group_created"
	activityCustomerGroupUpdated = "customer_group_updated"
	activityCustomerGroupDeleted = "customer_group_archived"
)

type CustomerGroupsConsumer struct {
	manager *manager.GroupsManager
}

func NewCustomerGroupsConsumer(groupsManager *manager.GroupsManager) *CustomerGroupsConsumer {
	return &CustomerGroupsConsumer{groupsManager}
}

func (c CustomerGroupsConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	log.Printf("New activity received: %s: %s", activity.Type(), activity.Data())

	var group *responses.CustomerGroupResponse

	switch activity.Type() {
	case activityCustomerGroupCreated, activityCustomerGroupUpdated, activityCustomerGroupDeleted:
		group, err = shared.NewCustomerGroupFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode customer group from activity: %s", err.Error())
		}
	}

	switch activity.Type() {
	case activityCustomerGroupCreated, activityCustomerGroupUpdated:
		return c.manager.ProcessChangedGroup(group)
	case activityCustomerGroupDeleted:
		return c.manager.ProcessDeletedGroup(group)
	default:
		return nil
	}
}
