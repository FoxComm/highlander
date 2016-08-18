package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"

	"github.com/FoxComm/metamorphosis"
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

	fullOrder, err := NewFullOrderFromActivity(activity)
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
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error creating shipment with error: %s", err.Error())
	}

	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		errResp, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return fmt.Errorf(
				"Failed to create shipment. Unable to read response with error %s",
				err.Error(),
			)
		}

		return fmt.Errorf(
			"Failed to create shipment with error %s",
			string(errResp),
		)
	}

	log.Printf("Created shipment(s) for order %s", order.ReferenceNumber)
	return nil
}
