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
	mwhURL string
}

func NewShipmentHandler(mwhURL string) (*ShipmentHandler, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}

	return &ShipmentHandler{mwhURL}, nil
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

	log.Printf("Found a newly shipped shipment!")
	return nil
}
