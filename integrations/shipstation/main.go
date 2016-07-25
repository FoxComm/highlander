package main

import (
	"fmt"
	"log"
	"os"

	"github.com/FoxComm/shipstation/lib/kafka"
	"github.com/FoxComm/shipstation/lib/phoenix"
	"github.com/FoxComm/shipstation/lib/shipstation"
	"github.com/FoxComm/shipstation/lib/shipstation/payloads"
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

func defaultMessageHandler(message *[]byte) error {
	order, err := phoenix.NewOrderFromAvro(*message)
	if err != nil {
		log.Printf("Unable to decode Avro message with error %s", err.Error())
		return nil
	}

	if order.State == "fulfillmentStarted" {
		ssOrder, err := toShipStationOrder(order)
		if err != nil {
			log.Printf("Unable to create ShipStation order with error %s", err.Error())
			return nil
		}

		key := os.Getenv("API_KEY")
		secret := os.Getenv("API_SECRET")

		client, err := shipstation.NewClient(key, secret)
		if err != nil {
			log.Panicf("Unable to create ShipStation client with error %s", err.Error())
		}

		_, err = client.CreateOrder(ssOrder)
		if err != nil {
			log.Panicf("Unable to create order in ShipStation with error %s", err.Error())
		}
	}

	return nil
}

func toShipStationOrder(order *phoenix.Order) (*payloads.Order, error) {
	if len(*order.BillingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a billing address", order.ReferenceNumber)
	}

	if len(*order.ShippingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a shipping address", order.ReferenceNumber)
	}

	billingAddress := (*order.BillingAddresses)[0]
	shippingAddress := (*order.ShippingAddresses)[0]

	ssOrder := payloads.Order{
		OrderNumber:      order.ReferenceNumber,
		OrderDate:        order.PlacedAt,
		OrderStatus:      "awaiting_shipment",
		CustomerUsername: &(order.Customer.Name),
		CustomerEmail:    &(order.Customer.Email),
		BillTo:           createShipStationAddress(order.Customer.Name, billingAddress),
		ShipTo:           createShipStationAddress(order.Customer.Name, shippingAddress),
		Items:            createShipStationItems(order.LineItems),
		TaxAmount:        float64(order.TaxesTotal) / 100,
		ShippingAmount:   float64(order.ShippingTotal) / 100,
		Weight:           payloads.Weight{Value: 2, Units: "ounces"},
		Dimensions:       payloads.Dimensions{Length: 1.0, Width: 1.0, Height: 1.0, Units: "inches"},
	}

	return &ssOrder, nil
}

func createShipStationAddress(name string, address phoenix.Address) payloads.Address {
	street2 := new(string)

	if address.Address2 != "" {
		street2 = &(address.Address2)
	}

	return payloads.Address{
		Name:    name,
		Street1: address.Address1,
		Street2: street2,
		City:    address.City,
		// State:       address.Region,
		State:      "WA",
		PostalCode: address.Zip,
		// Country:     address.Country,
		Country:     "US",
		Residential: true,
	}
}

func createShipStationItems(items *[]phoenix.OrderLineItem) []payloads.OrderItem {
	condensedItems := make(map[string]payloads.OrderItem)

	for _, item := range *items {
		ci, ok := condensedItems[item.SKU]
		if ok {
			ci.Quantity++
			condensedItems[item.SKU] = ci
		} else {
			condensedItems[item.SKU] = createShipStationItem(item)
		}
	}

	orderItems := []payloads.OrderItem{}
	for _, orderItem := range condensedItems {
		orderItems = append(orderItems, orderItem)
	}

	return orderItems
}

func createShipStationItem(item phoenix.OrderLineItem) payloads.OrderItem {
	return payloads.OrderItem{
		LineItemKey: item.ReferenceNumber,
		SKU:         item.SKU,
		Name:        item.Name,
		UnitPrice:   float64(item.Price) / 100.0,
		Quantity:    1,
		Weight:      payloads.Weight{Value: 1, Units: "ounces"},
	}
}
