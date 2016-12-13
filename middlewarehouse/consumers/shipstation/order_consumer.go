package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityOrderStateChanged     = "order_state_changed"
	activityOrderBulkStateChanged = "order_bulk_state_changed"
	orderStateFulfillmentStarted  = "fulfillmentStarted"
)

type OrderConsumer struct {
	phoenixClient phoenix.PhoenixClient
	ssClient      *api.Client
}

func NewOrderConsumer(phoenixClient phoenix.PhoenixClient, ssClient *api.Client) (*OrderConsumer, error) {
	return &OrderConsumer{phoenixClient, ssClient}, nil
}

func (c OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Panicf("Unable to decode Avro message with error %s", err.Error())
	}

	switch activity.Type() {
	case activityOrderStateChanged:
		fullOrder, err := shared.NewFullOrderFromActivity(activity)
		if err != nil {
			log.Panicf("Unable to decode order from activity")
		}

		return c.handlerInner(fullOrder)
	case activityOrderBulkStateChanged:
		bulkStateChange, err := shared.NewOrderBulkStateChangeFromActivity(activity)
		if err != nil {
			log.Panicf("Unable to decode bulk state change activity")
		}

		if bulkStateChange.NewState != orderStateFulfillmentStarted {
			return nil
		}

		if len(bulkStateChange.CordRefNums) == 0 {
			return nil
		}

		// Get orders from Phoenix
		orders, err := bulkStateChange.GetRelatedOrders(c.phoenixClient)
		if err != nil {
			log.Panicf("Error getting orders from Phoenix: %s", err.Error())
		}

		// Handle each order
		for _, fullOrder := range orders {
			c.handlerInner(fullOrder)
		}

		return nil
	default:
		return nil
	}
}

// Handle activity for single order
func (c OrderConsumer) handlerInner(fullOrder *shared.FullOrder) error {
	if fullOrder.Order.OrderState != orderStateFulfillmentStarted {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Add to ShipStation!",
		fullOrder.Order.ReferenceNumber,
	)

	ssOrder, err := payloads.NewOrderFromActivity(fullOrder.Order)
	if err != nil {
		log.Panicf("Unable to create ShipStation order with error %s", err.Error())
	}

	_, err = c.ssClient.CreateOrder(ssOrder)
	if err != nil {
		log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
	}

	return nil
}
