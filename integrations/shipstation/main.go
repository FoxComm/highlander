package main

import (
	"encoding/json"
	"errors"
	"fmt"

	"github.com/FoxComm/shipstation/lib/kafka"

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

type Shipment struct {
	AdjustmentsTotal       int    `json:"adjustments_total"`
	Assignees              string `json:"assignees"`
	AssignmentCount        int    `json:"assignment_count"`
	BillingAddresses       string `json:"billing_addresses"`
	BillingAddressesCount  int    `json:"billing_addresses_count"`
	CreatedAt              string `json:"created_at"`
	CreditCardCount        int    `json:"credit_card_count"`
	CreditCardTotal        int    `json:"credit_card_total"`
	Currency               string `json:"currency"`
	Customer               string `json:"customer"`
	GiftCardCount          int    `json:"gift_card_count"`
	GiftCardTotal          int    `json:"gift_card_total"`
	GrandTotal             int    `json:"grand_total"`
	ID                     int    `json:"id"`
	LineItemCount          int    `json:"line_item_count"`
	LineItems              string `json:"line_items"`
	Payments               string `json:"payments"`
	PlacedAt               string `json:"placed_at"`
	ReferenceNumber        string `json:"reference_number"`
	ReturnCount            int    `json:"return_count"`
	Returns                string `json:"returns"`
	ShipmentCount          int    `json:"shipment_count"`
	Shipments              string `json:"shipments"`
	ShippingAddresses      string `json:"shipping_addresses"`
	ShippingAddressesCount int    `json:"shipping_addresses_count"`
	ShippingTotal          int    `json:"shipping_total"`
	State                  string `json:"state"`
	StoreCreditCount       int    `json:"store_credit_count"`
	StoreCreditTotal       int    `json:"store_credit_total"`
	SubTotal               int    `json:"sub_total"`
	TaxesTotal             int    `json:"taxes_total"`
}

type Customer struct {
	Name  string
	Email string
}

type LineItem struct {
	SKU             string `json:"sku"`
	Name            string
	Price           int
	State           string
	ReferenceNumber string `json:"reference_number"`
}

type Address struct {
	Zip       string
	City      string
	Region    string
	Country   string
	Address1  string
	Address2  string
	Currency  string
	Continent string
}

func defaultMessageHandler(message *go_kafka_client.Message) error {
	record, ok := message.DecodedValue.(*goavro.GenericRecord)
	if !ok {
		return errors.New("Error decoding Avro message.")
	}

	recordStr := fmt.Sprintf("%v\n", record)
	bytes := []byte(recordStr)
	shipment := new(Shipment)

	if err := json.Unmarshal(bytes, &shipment); err != nil {
		panic(err)
	}

	if shipment.State == "fulfillmentStarted" {
		customerBytes := []byte(shipment.Customer)
		customer := new(Customer)

		if err := json.Unmarshal(customerBytes, customer); err != nil {
			fmt.Println("Error unmarshalling Customer")
			fmt.Printf("%s\n", shipment.Customer)
			panic(err)
		}

		lineItemBytes := []byte(shipment.LineItems)
		lineItems := new([]LineItem)

		if err := json.Unmarshal(lineItemBytes, lineItems); err != nil {
			fmt.Println("Error unmarshalling Line Items")
			fmt.Printf("%s\n", shipment.LineItems)
			panic(err)
		}

		billingAddressBytes := []byte(shipment.BillingAddresses)
		billingAddresses := new([]Address)

		if err := json.Unmarshal(billingAddressBytes, billingAddresses); err != nil {
			fmt.Println("Error unmarshalling Billing Addresses")
			fmt.Printf("%s\n", shipment.BillingAddresses)
			panic(err)
		}

		shippingAddressBytes := []byte(shipment.ShippingAddresses)
		shippingAddresses := new([]Address)

		if err := json.Unmarshal(shippingAddressBytes, shippingAddresses); err != nil {
			fmt.Println("Error unmarshalling Shipping Addresses")
			fmt.Printf("%s\n", shipment.ShippingAddresses)
			panic(err)
		}

		fmt.Printf("Ref: %s, State: %s\n", shipment.ReferenceNumber, shipment.State)
		fmt.Printf("Customer Email: %s\n", customer.Email)
		fmt.Printf("Line Item Count: %d\n", len(*lineItems))
		fmt.Printf("Billing Address Count: %d\n", len(*billingAddresses))
		fmt.Printf("Shipping Address Count: %d\n", len(*shippingAddresses))
	}

	return nil
}
