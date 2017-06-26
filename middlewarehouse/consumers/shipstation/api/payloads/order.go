package payloads

import (
	"fmt"

	mwhPayloads "github.com/FoxComm/highlander/middlewarehouse/api/payloads"
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

func NewOrderFromActivity(order mwhPayloads.Order) (*Order, error) {
	shippingAddress, err := NewAddressFromPhoenix(order.ShippingAddress.Name, *order.ShippingAddress)
	if err != nil {
		return nil, err
	}

	var billingAddress *Address
	for _, paymentMethod := range order.PaymentMethods {
		if paymentMethod.Type == "creditCard" {
			if paymentMethod.Address != nil {
				name := pickName(&order, &(paymentMethod.Address.Name))
				billingAddress, _ = NewAddressFromPhoenix(*name, *paymentMethod.Address)
			} else {
				return nil, fmt.Errorf("Order %s has credit card payment without an address", order.ReferenceNumber)
			}
		}
	}

	if billingAddress == nil {
		billingAddress = shippingAddress
	}

	name := pickName(&order, &(billingAddress.Name))

	ssOrder := Order{
		OrderNumber:      order.ReferenceNumber,
		OrderDate:        order.PlacedAt,
		OrderStatus:      awaitingShipmentStatus,
		CustomerUsername: name,
		CustomerEmail:    &(order.Customer.Email),
		BillTo:           *billingAddress,
		ShipTo:           *shippingAddress,
		Items:            NewOrderItemsFromPhoenix(order.LineItems.SKUs),
		TaxAmount:        float64(order.Totals.Taxes) / 100,
		ShippingAmount:   float64(order.Totals.Shipping) / 100,
		Weight:           Weight{Value: 2, Units: "ounces"},
		Dimensions:       Dimensions{Length: 1.0, Width: 1.0, Height: 1.0, Units: "inches"},
	}

	return &ssOrder, nil
}

func pickName(order *mwhPayloads.Order, addressName *string) *string {
	var name *string
	if len(order.Customer.Name) > 0 {
		name = &(order.Customer.Name)
	} else if len(*addressName) > 0 {
		name = addressName
	} else {
		name = &(order.ShippingAddress.Name)
	}
	return name
}
