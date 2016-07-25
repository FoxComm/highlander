package main

import (
	"errors"
	"fmt"

	"github.com/FoxComm/shipstation/lib/kafka"
	"github.com/FoxComm/shipstation/lib/phoenix"

	goavro "github.com/elodina/go-avro"
	"github.com/elodina/go_kafka_client"
)

func main() {
	zookeeper := "localhost:2181"
	schemaRepo := "http://localhost:8081"

	consumer, err := kafka.NewConsumer(zookeeper, schemaRepo, defaultMessageHandler)
	if err != nil {
		panic(err)
	}

	consumer.RunTopic("orders_search_view")
}

func defaultMessageHandler(message *go_kafka_client.Message) error {
	record, ok := message.DecodedValue.(*goavro.GenericRecord)
	if !ok {
		return errors.New("Error decoding Avro message.")
	}

	recordStr := fmt.Sprintf("%v\n", record)
	bytes := []byte(recordStr)

	order, err := phoenix.NewOrderFromAvro(bytes)
	if err != nil {
		panic(err)
	}

	fmt.Printf("Ref: %s, State: %s\n", order.ReferenceNumber, order.State)
	fmt.Printf("Customer Email: %s\n", order.Customer.Email)
	fmt.Printf("Line Item Count: %d\n", len(*order.LineItems))
	fmt.Printf("Billing Address Count: %d\n", len(*order.BillingAddresses))
	fmt.Printf("Shipping Address Count: %d\n", len(*order.ShippingAddresses))

	return nil
}
