package main

import (
	"errors"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix/payloads"
	"github.com/FoxComm/metamorphosis"
)

const activityShipmentShipped = "shipment_shipped"

type ShipmentHandler struct {
	mwhURL string
	client phoenix.PhoenixClient
}

func NewShipmentHandler(mwhURL string, client phoenix.PhoenixClient) (*ShipmentHandler, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}

	return &ShipmentHandler{mwhURL, client}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func (h ShipmentHandler) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type() != activityShipmentShipped {
		return nil
	}
	capture, err := payloads.NewCapturePayload(activity)
	if err != nil {
		return err
	}
	if err := h.client.CapturePayment(capture); err != nil {
		log.Printf("Unable to capture payment with error: %s", err.Error())
		return err
	}

	return nil
}
