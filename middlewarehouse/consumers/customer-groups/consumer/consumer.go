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

	"github.com/FoxComm/highlander/middlewarehouse/shared/mailchimp"
	"gopkg.in/olivere/elastic.v3"
)

const (
	activityCustomerGroupCreated = "customer_group_created"
	activityCustomerGroupUpdated = "customer_group_updated"
	activityCustomerGroupDeleted = "customer_group_archived"
)

const (
	DefaultTopic           = "activities"
	DefaultElasticTopic    = "customers_search_view"
	DefaultElasticSize     = 100
	DefaultMailchimpListID = ""
)

type CustomerGroupsConsumer struct {
	esClient      *elastic.Client
	phoenixClient phoenix.PhoenixClient
	chimpClient   *mailchimp.ChimpClient
	esTopic       string
	esSize        int
	chimpListID   string
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

func SetMailchimpListID(id string) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.chimpListID = id
	}
}

func NewCustomerGroupsConsumer(esClient *elastic.Client,
	phoenixClient phoenix.PhoenixClient,
	chimpClient *mailchimp.ChimpClient,
	options ...ConsumerOptionFunc) (*CustomerGroupsConsumer, error) {

	consumer := &CustomerGroupsConsumer{
		esClient,
		phoenixClient,
		chimpClient,
		DefaultElasticTopic,
		DefaultElasticSize,
		DefaultMailchimpListID,
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

		if group.GroupType == "manual" {
			log.Printf("Group %s with id %d is manual, skipping.\n", group.Name, group.ID)

			return nil
		}

		return manager.ProcessChangedGroup(c.esClient, c.phoenixClient, c.chimpClient, group, c.esTopic, c.esSize, c.chimpListID)
	case activityCustomerGroupDeleted:
		group, err = shared.NewCustomerGroupFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode customer group from activity: %s", err.Error())
		}

		return manager.ProcessDeletedGroup(c.chimpClient, group, c.chimpListID)
	default:
		return nil
	}
}
