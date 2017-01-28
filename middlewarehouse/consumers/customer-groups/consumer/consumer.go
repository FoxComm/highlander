package consumer

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityCustomerGroupCreated = "customer_group_created"
	activityCustomerGroupUpdated = "customer_group_updated"
	activityCustomerGroupDeleted = "customer_group_deleted"
)

const (
	DefaultPhoenixURL      = "http:/127.0.0.1:9090"
	DefaultPhoenixUser     = "api"
	DefaultPhoenixPassword = "password"
	DefaultTopic           = "activities"
)

type CustomerGroupsConsumer struct {
	phoenixClient phoenix.PhoenixClient
	phoenixURL    string
	phoenixUser   string
	phoenixPass   string
	topic         string
}

type ConsumerOptionFunc func(consumer *CustomerGroupsConsumer)

func SetPhoenixURL(url string) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.phoenixURL = url
	}
}

func SetPhoenixAuth(user, password string) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.phoenixUser = user
		c.phoenixPass = password
	}
}

func SetTopic(topic string) ConsumerOptionFunc {
	return func(c *CustomerGroupsConsumer) {
		c.topic = topic
	}
}

func NewCustomerGroupsConsumer(options ...ConsumerOptionFunc) (*CustomerGroupsConsumer, error) {
	consumer := &CustomerGroupsConsumer{
		nil,
		DefaultPhoenixURL,
		DefaultPhoenixUser,
		DefaultPhoenixPassword,
		DefaultTopic,
	}

	// set options to consumer
	for _, opt := range options {
		opt(consumer)
	}

	consumer.phoenixClient = phoenix.NewPhoenixClient(consumer.phoenixURL, consumer.phoenixUser, consumer.phoenixPass)

	return consumer, nil
}

func (c CustomerGroupsConsumer) Handler(message metamorphosis.AvroMessage) error {
	log.Printf("Running %s consumer", c.topic)
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Panicf("Unable to decode Avro message with error %s", err.Error())
	}

	log.Printf("New activity received: %s: %s", activity.Type(), activity.Data())

	var cg *payloads.CustomerGroupPayload

	switch activity.Type() {
	case activityCustomerGroupCreated, activityCustomerGroupUpdated, activityCustomerGroupDeleted:
		cg, err = shared.NewCustomerGroupFromActivity(activity)
		if err != nil {
			log.Panicf("Unable to decode customer group from activity: %s", err.Error())
		}

		log.Printf("Customer group request: %s", cg.ElasticRequest)
	default:
		return nil
	}

	return c.handlerInner(cg)
}

// Handle activity for single order
func (c CustomerGroupsConsumer) handlerInner(cg *payloads.CustomerGroupPayload) error {
	return nil
}
