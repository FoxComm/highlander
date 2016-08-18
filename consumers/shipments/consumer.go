package shipments

import (
	"fmt"
	"log"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/common/lib/phoenix"
)

const (
	activityOrderStateChanged    = "order_state_changed"
	orderStateFulfillmentStarted = "fulfillmentStarted"
)

// FulfilledOrderHandler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func FulfilledOrderHandler(message metamorphosis.AvroMessage) error {
	activity, err := NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type != activityOrderStateChanged {
		return nil
	}

	fullOrder, err := phoenix.NewFullOrderFromActivity(activity)
	if err != nil {
		return fmt.Errorf("Unable to decode order from activity with error %s", err.Error())
	}

	order := fullOrder.Order
	if order.OrderState != orderStateFulfillmentStarted {
		return nil
	}

	log.Printf(
		"Found order %s in fulfillmentStarted. Add to middlewarehouse!",
		order.ReferenceNumber,
	)

	return nil
}
