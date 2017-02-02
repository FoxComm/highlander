package consumer

import (
	"log"

	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/customer-groups/manager"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/responses"
	"github.com/FoxComm/metamorphosis"

	"gopkg.in/olivere/elastic.v3"
)

const (
	activityCustomerGroupCreated = "customer_group_created"
	activityCustomerGroupUpdated = "customer_group_updated"
)

const (
	DefaultTopic        = "activities"
	DefaultElasticTopic = "customers_search_view"
	DefaultElasticSize  = 100
)

type CustomerGroupsConsumer struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	esTopic       string
	esSize        int
}

type ConsumerOptionFunc func(consumer *CustomerGroupsConsumer)

func SetTopic(topic string) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.esTopic = topic
	}
}

func SetElasticQierySize(size int) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.esSize = size
	}
}

func NewCustomerGroupsConsumer(esClient *elastic.Client, phoenixClient phoenix.PhoenixClient, options ...ConsumerOptionFunc) (*CustomerGroupsConsumer, error) {
	consumer := &CustomerGroupsConsumer{
		esClient,
		phoenixClient,
		DefaultElasticTopic,
		DefaultElasticSize,
	}

	// set options to consumer
	for _, opt := range options {
		opt(consumer)
	}

	return consumer, nil
}

func (c CustomerGroupsConsumer) Handler(message metamorphosis.AvroMessage) error {
	log.Printf("Running %s consumer", c.esTopic)
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	log.Printf("New activity received: %s: %s", activity.Type(), activity.Data())

	var group *responses.CustomerGroupResponse

	switch activity.Type() {
	case activityCustomerGroupCreated, activityCustomerGroupUpdated:
		group, err = shared.NewCustomerGroupFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode customer group from activity: %s", err.Error())
		}

		log.Printf("Customer group request: %s", group.ElasticRequest)
	default:
		return nil
	}

	if group.GroupType == "manual" {
		log.Printf("Group %s with id %d is manual, skipping.\n", group.Name, group.ID)

		return nil
	}

	return c.handlerInner(group)
}

// Handle activity for single order
func (c CustomerGroupsConsumer) handlerInner(group *responses.CustomerGroupResponse) error {
	ids, err := manager.GetCustomersIDs(c.esClient, group, c.esTopic, c.esSize)
	if err != nil {
		return fmt.Errorf("An error occured getting customers: %s", err)
	}

	if err := c.phoenixClient.SetGroupToCustomers(group.ID, ids); err != nil {
		return fmt.Errorf("An error occured setting group to customers: %s", err)
	}

	if group.CustomersCount != len(ids) {
		if err := manager.UpdateGroup(c.phoenixClient, group, len(ids)); err != nil {
			return fmt.Errorf("An error occured update group info: %s", err)
		}
	}

	return nil
}
