package gift_cards

import ()

const (
    activityOrderStateChanged    = "order_state_changed"
    orderStateFulfillmentStarted = "fulfillmentStarted"
    orderStateShipped = "shipped"
)
// GiftCardConsumer represents a topic for giftcards
type GiftCardHandler struct {
    mwhURL string
}

//NewGiftCardConsumer creates a new consumer for gifcards
func NewGiftCardConsumer(topic string) (*GiftCardHandler, error) {
    if mwhURL == "" {
        return nil, errors.New("middlewarehouse URL must be set")
    }
    return &GiftCardHandler{topic}, nil
}


// Handler accepts an Avro encoded message from Kafka and takes
// based on the activities topic and looks for orders that were just placed in
// fulfillment started and shipped states. If it finds one, He will retrieve
// the order, manage the creation of the existent cards and make the capture. Returning an error will cause a panic.
func (o GiftCardHandler) Handler(message metamorphosis.AvroMessage) error {
    activity, err := activities.NewActivityFromAvro(message)
    if err != nil {
        return fmt.Errorf("Unable to decode Avro message with error %s", err.Error())
    }

    if activity.Type() != activityOrderStateChanged {
        return nil
    }

    fullOrder, err := NewFullOrderFromActivity(activity)
    if err != nil {
        return fmt.Errorf("Unable to decode order from activity with error %s", err.Error())
    }

    order := fullOrder.Order
    if order.OrderState != orderStateFulfillmentStarted || order.OrderState == orderStateShipped {
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

    /*url := fmt.Sprintf("%s/v1/public/shipments/from-order", o.mwhURL)
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
    }*/
    log.Printf("Created shipment(s) for order %s", order.ReferenceNumber)
    return nil
}

