package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityOrderStateChanged    = "order_state_changed"
	orderStateFulfillmentStarted = "fulfillmentStarted"
)

type OrderHandler struct {
	mwhURL string
}

func NewOrderHandler(mwhURL string) (*OrderHandler, exceptions.IException) {
	if mwhURL == "" {
		return nil, NewShipmentsConsumerException(errors.New("middlewarehouse URL must be set"))
	}

	return &OrderHandler{mwhURL}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func (o OrderHandler) Handler(message metamorphosis.AvroMessage) error {
	activity, exception := activities.NewActivityFromAvro(message)
	if exception != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", exception.ToString())
	}

	if activity.Type() != activityOrderStateChanged {
		return nil
	}

	fullOrder, exception := shared.NewFullOrderFromActivity(activity)
	if exception != nil {
		return fmt.Errorf("Unable to decode order from activity with error %s", exception.ToString())
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

	url := fmt.Sprintf("%s/v1/public/shipments/from-order", o.mwhURL)
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

type shipmentsConsumerException struct {
	Type string `json:"type"`
	exceptions.Exception
}

func (exception shipmentsConsumerException) ToJSON() interface{} {
	return exception
}

func NewShipmentsConsumerException(error error) exceptions.IException {
	if error == nil {
		return nil
	}

	return shipmentsConsumerException{
		Type:       "shipmentsConsumer",
		Exception: exceptions.Exception{error.Error()},
	}
}
