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
	if !((order.OrderState == orderStateFulfillmentStarted && justGiftCards(skus)) ||
		(order.OrderState == orderStateShipped && !justGiftCards(skus))) {
		return nil
	}

	giftcardPayloads := make([]payloads.CreateGiftCardPayload, 0)
	if order.OrderState == orderStateFulfillmentStarted && justGiftCards(skus) {
		log.Printf("state fulfillment started and  just giftcards")
		for _, sku := range skus {
			for j := 0; j < sku.Quantity; j++ {
				giftcardPayloads = append(giftcardPayloads, payloads.CreateGiftCardPayload{
					Balance: sku.Price,
					Details: sku.Attributes.GiftCard,
					CordRef: order.ReferenceNumber})
			}
		}

	} else if justGiftCards(skus) == false {
		log.Printf("state shipped and not just giftcards")
		for _, sku := range skus {
			if sku.Attributes != nil {
				giftcardPayloads = append(giftcardPayloads, payloads.CreateGiftCardPayload{
					Balance: sku.Price,
					Details: sku.Attributes.GiftCard,
					CordRef: order.ReferenceNumber,
				})
			}
		}
	}

	log.Printf("\n about to call createGiftCards service")
	_, err = gfHandle.client.CreateGiftCards(giftcardPayloads)
	if err != nil {
		return fmt.Errorf("Unable to create the Giftcards for order  %s with error %s",
			order.ReferenceNumber, err.Error())
	}

	log.Printf("\n about to create capture payload")
	capturePayload, err := lib.NewGiftCardCapturePayload(order.ReferenceNumber, skus)
	if err != nil {
		return fmt.Errorf("\nUnable to create Capture payload for  %s with error %s",
			order.ReferenceNumber, err.Error())
	}

	log.Printf("\n about to capture")
	err = gfHandle.client.CapturePayment(capturePayload)
	if err != nil {
		return fmt.Errorf("Unable to capture the payment for  %s with error %s",
			order.ReferenceNumber, err.Error())
	}

	log.Printf("Gift cards created successfully for order %s", order.ReferenceNumber)
	return nil
}
