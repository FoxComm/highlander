package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
	"github.com/FoxComm/metamorphosis"
)

const (
	activityOrderStateChanged     = "order_state_changed"
	activityOrderBulkStateChanged = "order_bulk_state_changed"
	orderStateShipped             = "shipped"
)

// GiftCardConsumer represents a consumer for giftcards
type GiftCardConsumer struct {
	client lib.PhoenixClient
}

//NewGiftCardConsumer creates a new consumer for gifcards
func NewGiftCardConsumer(client lib.PhoenixClient) (*GiftCardConsumer, error) {
	return &GiftCardConsumer{client}, nil
}

// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started and shipped states. If it finds one, He will retrieve
// the order, manage the creation of the existent cards and make the capture. Returning an error will cause a panic.
func (consumer GiftCardConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
	}

	log.Printf("New activity %s", activity.Data())

	switch activity.Type() {
	case activityOrderStateChanged:
		fullOrder, err := shared.NewFullOrderFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode order from activity with error %s", err.Error())
		}

		return consumer.processOrder(fullOrder.Order)
	case activityOrderBulkStateChanged:
		bulkStateChange, err := shared.NewOrderBulkStateChangeFromActivity(activity)
		if err != nil {
			return fmt.Errorf("Unable to decode bulk state change activity with error %s", err.Error())
		}

		if bulkStateChange.NewState != orderStateShipped || len(bulkStateChange.CordRefNums) == 0 {
			return nil
		}

		for _, refNum := range bulkStateChange.CordRefNums {
			payload, err := consumer.client.GetOrder(refNum)
			if err != nil {
				log.Printf("Cannot retrieve order %s with error %s", refNum, err)
				continue
			}

			fullOrder := shared.NewFullOrderFromPayload(payload)

			if err = consumer.processOrder(fullOrder.Order); err != nil {
				log.Printf("Cannot create GC for order %s with error %s", refNum, err)
			}
		}

		return nil
	default:
		return nil
	}
}

// Handle activity for single order
func (consumer GiftCardConsumer) processOrder(order payloads.Order) error {
	// We now only need to deal with orders that have been shipped.
	if order.OrderState != orderStateShipped {
		return nil
	}

	giftcardPayloads := make([]payloads.CreateGiftCardPayload, 0)
	skusToUpdate := make([]payloads.OrderLineItem, 0)

	log.Printf("Creating giftcards for all line-items with GCs in order %s", order.ReferenceNumber)

	for _, sku := range order.LineItems.SKUs {
		if sku.Attributes != nil && sku.Attributes.GiftCard != nil {
			if invalidAttributes(sku.Attributes.GiftCard) {
				log.Printf("Unable to create gift cards for order %s, payload malformed", order.ReferenceNumber)
			}

			skusToUpdate = append(skusToUpdate, sku)
			for j := 0; j < sku.Quantity; j++ {
				giftcardPayloads = append(giftcardPayloads, *payloads.NewCreateGiftCardPayload(sku, order.ReferenceNumber))
			}
		}
	}

	if len(giftcardPayloads) > 0 {
		if err := consumer.processGiftCards(giftcardPayloads, skusToUpdate, order); err != nil {
			log.Printf("Unable to create GCs for order %s, with error %s", order.ReferenceNumber, err.Error())
		} else {
			log.Printf("GCs created successfully for order %s", order.ReferenceNumber)
		}
	}
	return nil
}

func (consumer GiftCardConsumer) processGiftCards(giftCardPayloads []payloads.CreateGiftCardPayload, skusToUpdate []payloads.OrderLineItem, order payloads.Order) error {
	giftCardResponse, err := consumer.client.CreateGiftCards(giftCardPayloads)
	defer giftCardResponse.Body.Close()

	if err != nil {
		return fmt.Errorf("Unable to create GCs for order %s with error %s", order.ReferenceNumber, err.Error())
	}

	codes := make([]payloads.GiftCardResponse, 0)

	if err := json.NewDecoder(giftCardResponse.Body).Decode(&codes); err != nil {
		return fmt.Errorf("Unable to get GCs codes for order %s with error %s", order.ReferenceNumber, err.Error())
	}

	updateOrderLineItemsPayloads := make([]payloads.UpdateOrderLineItem, 0)
	for i, sku := range skusToUpdate {
		sku.Attributes.GiftCard.Code = &codes[i].Code

		for _, refNum := range sku.ReferenceNumbers {
			updateOrderLineItemsPayloads = append(updateOrderLineItemsPayloads, *payloads.NewUpdateOrderLineItem(sku, refNum))
		}

		fmt.Printf("%#v", updateOrderLineItemsPayloads)
	}

	if err := consumer.client.UpdateOrderLineItems(updateOrderLineItemsPayloads, order.ReferenceNumber); err != nil {
		return fmt.Errorf("Unable to update order line items for order %s with error %s", order.ReferenceNumber, err.Error())
	}

	return nil
}

func invalidAttributes(giftCard *payloads.GiftCard) bool {
	return giftCard.SenderName == "" || giftCard.RecipientName == "" || giftCard.RecipientEmail == ""
}
