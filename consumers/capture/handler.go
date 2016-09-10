package main

import (
	"errors"
	"fmt"
	"log"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/models/activities"
)

const activityShipmentShipped = "shipment_shipped"

type ShipmentHandler struct {
	mwhURL     string
	phoenixURL string
	phoenixJWT string
}

func NewShipmentHandler(mwhURL, phoenixURL, phoenixJWT string) (*ShipmentHandler, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}

	if phoenixURL == "" {
		return nil, errors.New("Phoenix URL must be set")
	}

	if phoenixJWT == "" {
		return nil, errors.New("Phoenix JWT must be set")
	}

	return &ShipmentHandler{mwhURL, phoenixURL, phoenixJWT}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started. If it finds one, it sends to middlewarehouse to create
// a shipment. Returning an error will cause a panic.
func (o ShipmentHandler) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type() != activityShipmentShipped {
		return nil
	}

	if err := CapturePayment(activity, "http://127.0.0.1:9090", "JWT"); err != nil {
		log.Printf("Unable to capture payment with error: %s", err.Error())
		return err
	}

	return nil
}
