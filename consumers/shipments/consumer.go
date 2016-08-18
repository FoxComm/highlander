package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"

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

	b, err := json.Marshal(&order)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/shipments/from-order", "http://localhost:9292")
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
	if err != nil {
		return err
	}

	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	if _, err := client.Do(req); err != nil {
		log.Printf("Error creating shipment with error: %s", err.Error())
	}

	log.Printf("Created shipment(s) for order %s", order.ReferenceNumber)
	return nil
}
