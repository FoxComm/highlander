package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityOrderStateChanged    = "order_state_changed"
	orderStateFulfillmentStarted = "fulfillmentStarted"
	orderStateShipped            = "shipped"
)

// GiftCardHandler represents a topic for giftcards
type GiftCardHandler struct {
	client lib.PhoenixClient
}

//NewGiftCardConsumer creates a new consumer for gifcards
func NewGiftCardConsumer(client lib.PhoenixClient) (*GiftCardHandler, error) {
	return &GiftCardHandler{client}, nil
}

func justGiftCards(oli []payloads.OrderLineItem) bool {
	for _, lineItem := range oli {
		if lineItem.Attributes == nil {
			return false
		}
	}
	return true
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started and shipped states. If it finds one, He will retrieve
// the order, manage the creation of the existent cards and make the capture. Returning an error will cause a panic.
func (gfHandle GiftCardHandler) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	if activity.Type() != activityOrderStateChanged {
		return nil
	}

	fullOrder, err := shared.NewFullOrderFromActivity(activity)
	if err != nil {
		return fmt.Errorf("Unable to decode order from activity with error %s", err.Error())
	}

	order := fullOrder.Order
	lineItems := order.LineItems
	skus := lineItems.SKUs

	//We now only need to deal with orders that have been shipped.
	//Once they are shipped and captured, we will create the giftcards here.
	if order.OrderState != orderStateShipped {
		return nil
	}

	giftcardPayloads := make([]payloads.CreateGiftCardPayload, 0)

	log.Printf("Creating giftcards for all gift-card-line-items in order")
	for _, sku := range skus {
		if sku.Attributes != nil {
			for j := 0; j < sku.Quantity; j++ {
				giftcardPayloads = append(giftcardPayloads, payloads.CreateGiftCardPayload{
					Balance: sku.Price,
					Details: sku.Attributes.GiftCard,
					CordRef: order.ReferenceNumber,
				})
			}
		}
	}

	log.Printf("\n about to call createGiftCards service")

	if len(giftcardPayloads) > 0 {
		_, err = gfHandle.client.CreateGiftCards(giftcardPayloads)
		if err != nil {
			return fmt.Errorf("Unable to create gift cards for order %s with error %s",
				order.ReferenceNumber, err.Error())
		}
		log.Printf("Gift cards created successfully for order %s", order.ReferenceNumber)
	}

	return nil
}
