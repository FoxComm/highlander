package main

import (
	"errors"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

const activityShipmentShipped = "shipment_shipped"

type ShipmentHandler struct {
	mwhURL string
	client lib.PhoenixClient
}

func NewShipmentHandler(mwhURL string, client lib.PhoenixClient) (*ShipmentHandler, exceptions.IException) {
	if mwhURL == "" {
		return nil, exceptions.NewBadConfigurationException(errors.New("middlewarehouse URL must be set"))
	}

	return &ShipmentHandler{mwhURL, client}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func (h ShipmentHandler) Handler(message metamorphosis.AvroMessage) error {
	activity, exception := activities.NewActivityFromAvro(message)
	if exception != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", exception.ToString())
	}

	if activity.Type() != activityShipmentShipped {
		return nil
	}
	capture, exception := lib.NewCapturePayload(activity)
	if exception != nil {
		return errors.New(exception.ToString())
	}
	if exception := h.client.CapturePayment(capture); exception != nil {
		log.Printf("Unable to capture payment with error: %s", exception.ToString())
		return errors.New(exception.ToString())
	}

	return nil
}
