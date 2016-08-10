package utils

import (
	"fmt"

	"github.com/FoxComm/shipstation/lib/phoenix"
	"github.com/FoxComm/shipstation/lib/shipstation/payloads"
)

func ToShipStationOrder(order *phoenix.Order) (*payloads.Order, error) {
	if len(*order.BillingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a billing address", order.ReferenceNumber)
	}

	if len(*order.ShippingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a shipping address", order.ReferenceNumber)
	}

	billingAddress, err := createShipStationAddress(order.Customer.Name, (*order.BillingAddresses)[0])
	if err != nil {
		return nil, err
	}

	shippingAddress, err := createShipStationAddress(order.Customer.Name, (*order.ShippingAddresses)[0])
	if err != nil {
		return nil, err
	}

	ssOrder := payloads.Order{
		OrderNumber:      order.ReferenceNumber,
		OrderDate:        order.PlacedAt,
		OrderStatus:      "awaiting_shipment",
		CustomerUsername: &(order.Customer.Name),
		CustomerEmail:    &(order.Customer.Email),
		BillTo:           billingAddress,
		ShipTo:           shippingAddress,
		Items:            createShipStationItems(order.LineItems),
		TaxAmount:        float64(order.TaxesTotal) / 100,
		ShippingAmount:   float64(order.ShippingTotal) / 100,
		Weight:           payloads.Weight{Value: 2, Units: "ounces"},
		Dimensions:       payloads.Dimensions{Length: 1.0, Width: 1.0, Height: 1.0, Units: "inches"},
	}

	return &ssOrder, nil
}

func createShipStationAddress(name string, address phoenix.Address) (payloads.Address, error) {
	street2 := new(string)

	if address.Address2 != "" {
		street2 = &(address.Address2)
	}

	state, err := convertRegionFromPhoenix(address.Region)
	if err != nil {
		return payloads.Address{}, err
	}

	country, err := convertCountryFromPhoenix(address.Country)
	if err != nil {
		return payloads.Address{}, err
	}

	return payloads.Address{
		Name:        name,
		Street1:     address.Address1,
		Street2:     street2,
		City:        address.City,
		State:       state,
		PostalCode:  address.Zip,
		Country:     country,
		Residential: true,
	}, nil
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
