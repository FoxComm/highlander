package consumers

import (
    "fmt"
    "log"

    "github.com/FoxComm/highlander/giftcard-consumer/lib/phoenix"
    "github.com/FoxComm/metamorphosis"
)

type GiftCardConsumer struct {
    topic string
}

func NewGiftCardConsumer(topic string) (*GiftCardConsumer, error) {
    //key := os.Getenv("API_KEY")
    //secret := os.Getenv("API_SECRET")

    return &GiftCardConsumer{topic}, nil
}

func (c GiftCardConsumer) Handler(message metamorphosis.AvroMessage) error {
    log.Printf("Received a new message from %s", c.topic)

    order, err := phoenix.NewOrderFromAvro(message)
    if err != nil {
        log.Panicf("Unable to decode Avro message with error %s", err.Error())
    }

    if order.State == "fulfillmentStarted" {
        log.Printf("Handling order with reference number %s", order.ReferenceNumber)
        fmt.Printf("%v", order.LineItems)

    }

    return nil
}
