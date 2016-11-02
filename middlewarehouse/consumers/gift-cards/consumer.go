package main

import (
	"errors"
	"fmt"

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
	mwhURL string
	client lib.PhoenixClient
}

//NewGiftCardConsumer creates a new consumer for gifcards
func NewGiftCardConsumer(mwhURL string, client lib.PhoenixClient) (*GiftCardHandler, error) {
	if mwhURL == "" {
		return nil, errors.New("middlewarehouse URL must be set")
	}
	return &GiftCardHandler{mwhURL, client}, nil
}

func justGiftCards(oli []payloads.OrderLineItem) bool {
	justGF := true
	for i := 0; i < len(oli) && justGF; i++ {
		justGF = oli[i].Attributes != nil
	}
	return justGF
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
	if order.OrderState == orderStateFulfillmentStarted || order.OrderState == orderStateShipped {
		lineItems := order.LineItems
		skus := lineItems.SKUs
		if order.OrderState == orderStateFulfillmentStarted && justGiftCards(skus) {
			for i := 0; i < len(skus); i++ {
				_, err := gfHandle.client.CreateGiftCard(skus[i].Price, skus[i].Attributes.GiftCard, order.ReferenceNumber)
				if err != nil {
					return fmt.Errorf("Unable to create the Giftcard for order  %s with error %s", order.ReferenceNumber, err.Error())
				}
			}
			capturePayload, err := lib.NewGiftCardCapturePayload(order.ReferenceNumber, order.LineItems.SKUs)
			if err != nil {
				return fmt.Errorf("Unable to create Capture payload for  %s with error %s", order.ReferenceNumber, err.Error())
			}
			gfHandle.client.GiftCardCapturePayment(capturePayload)
		} else if order.OrderState == orderStateShipped {
			var lineItemsToCapture []payloads.OrderLineItem
			y := 0
			for i := 0; i < len(skus); i++ {
				if skus[i].Attributes != nil {
					_, err := gfHandle.client.CreateGiftCard(skus[i].Price, skus[i].Attributes.GiftCard, order.ReferenceNumber)
					if err != nil {
						return fmt.Errorf("Unable to create the Giftcard for order  %s with error %s", order.ReferenceNumber, err.Error())
					}
					lineItemsToCapture[y] = skus[i]
				}
			}
			capturePayload, err := lib.NewGiftCardCapturePayload(order.ReferenceNumber, lineItemsToCapture)
			if err != nil {
				return fmt.Errorf("Unable to create Capture payload for  %s with error %s", order.ReferenceNumber, err.Error())
			}
			gfHandle.client.GiftCardCapturePayment(capturePayload)

		}
	}
	return nil
}
