package payloads

import (
	"fmt"

	"github.com/FoxComm/highlander/integrations/shipstation/lib/phoenix"
)

const awaitingShipmentStatus = "awaiting_shipment"

// Order respresents an order in ShipStation.
type Order struct {
	OrderNumber              string
	OrderKey                 *string
	OrderDate                string
	PaymentDate              *string
	ShipByDate               *string
	OrderStatus              string
	CustomerUsername         *string
	CustomerEmail            *string
	BillTo                   Address
	ShipTo                   Address
	Items                    []OrderItem
	AmountPaid               float64
	TaxAmount                float64
	ShippingAmount           float64
	CustomerNotes            string
	InternalNote             string
	Gift                     bool
	GiftMessage              string
	PaymentMethod            string
	RequestedShippingService string
	CarrierCode              string
	ServiceCode              string
	PackageCode              string
	Confirmation             string
	ShipDate                 string
	HoldUntilDate            string
	Weight                   Weight
	Dimensions               Dimensions
	InsuranceOptions         InsuranceOptions
	AdvancedOptions          AdvancedOptions
	TagIDs                   []int `json:"tagIds"`
}

func NewOrderFromPhoenix(order *phoenix.Order) (*Order, error) {
	if len(*order.BillingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a billing address", order.ReferenceNumber)
	}

	if len(*order.ShippingAddresses) == 0 {
		return nil, fmt.Errorf("Order %s does not have a shipping address", order.ReferenceNumber)
	}

	billingAddress, err := NewAddressFromPhoenix(order.Customer.Name, (*order.BillingAddresses)[0])
	if err != nil {
		return nil, err
	}

	shippingAddress, err := NewAddressFromPhoenix(order.Customer.Name, (*order.ShippingAddresses)[0])
	if err != nil {
		return nil, err
	}

	ssOrder := Order{
		OrderNumber:      order.ReferenceNumber,
		OrderDate:        order.PlacedAt,
		OrderStatus:      awaitingShipmentStatus,
		CustomerUsername: &(order.Customer.Name),
		CustomerEmail:    &(order.Customer.Email),
		BillTo:           billingAddress,
		ShipTo:           shippingAddress,
		Items:            NewOrderItemsFromPhoenix(*order.LineItems),
		TaxAmount:        float64(order.TaxesTotal) / 100,
		ShippingAmount:   float64(order.ShippingTotal) / 100,
		Weight:           Weight{Value: 2, Units: "ounces"},
		Dimensions:       Dimensions{Length: 1.0, Width: 1.0, Height: 1.0, Units: "inches"},
	}

	return &ssOrder, nil
}
