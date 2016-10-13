package phoenix

import (
	"encoding/json"

	"github.com/FoxComm/metamorphosis"
)

// Order represents the order object that exists in the orders_search_view.
type Order struct {
	AdjustmentsTotal       int              `json:"adjustments_total"`
	Assignees              string           `json:"assignees"`
	AssignmentCount        int              `json:"assignment_count"`
	BillingAddresses       *[]Address       `json:"billing_addresses"`
	BillingAddressesCount  int              `json:"billing_addresses_count"`
	CreatedAt              string           `json:"created_at"`
	CreditCardCount        int              `json:"credit_card_count"`
	CreditCardTotal        int              `json:"credit_card_total"`
	Currency               string           `json:"currency"`
	Customer               *Customer        `json:"customer"`
	GiftCardCount          int              `json:"gift_card_count"`
	GiftCardTotal          int              `json:"gift_card_total"`
	GrandTotal             int              `json:"grand_total"`
	ID                     int              `json:"id"`
	LineItemCount          int              `json:"line_item_count"`
	LineItems              *[]OrderLineItem `json:"line_items"`
	Payments               string           `json:"payments"`
	PlacedAt               string           `json:"placed_at"`
	ReferenceNumber        string           `json:"reference_number"`
	ReturnCount            int              `json:"return_count"`
	Returns                string           `json:"returns"`
	ShipmentCount          int              `json:"shipment_count"`
	Shipments              string           `json:"shipments"`
	ShippingAddresses      *[]Address       `json:"shipping_addresses"`
	ShippingAddressesCount int              `json:"shipping_addresses_count"`
	ShippingTotal          int              `json:"shipping_total"`
	State                  string           `json:"state"`
	StoreCreditCount       int              `json:"store_credit_count"`
	StoreCreditTotal       int              `json:"store_credit_total"`
	SubTotal               int              `json:"sub_total"`
	TaxesTotal             int              `json:"taxes_total"`
}

// NewOrderFromAvro consumes a decoded Avro message and unmarshals it into an
// Order object.
func NewOrderFromAvro(message metamorphosis.AvroMessage) (*Order, error) {
	o := new(order)
	if err := json.Unmarshal(message.Bytes(), o); err != nil {
		return nil, err
	}

	finalOrder := newOrder(o)

	customerBytes := []byte(o.Customer)
	customer := new(Customer)

	if err := json.Unmarshal(customerBytes, customer); err != nil {
		return nil, err
	}

	finalOrder.Customer = customer

	lineItemBytes := []byte(o.LineItems)
	lineItems := new([]OrderLineItem)

	if err := json.Unmarshal(lineItemBytes, lineItems); err != nil {
		return nil, err
	}

	finalOrder.LineItems = lineItems

	billingAddressBytes := []byte(o.BillingAddresses)
	billingAddresses := new([]Address)

	if err := json.Unmarshal(billingAddressBytes, billingAddresses); err != nil {
		return nil, err
	}

	finalOrder.BillingAddresses = billingAddresses

	shippingAddressBytes := []byte(o.ShippingAddresses)
	shippingAddresses := new([]Address)

	if err := json.Unmarshal(shippingAddressBytes, shippingAddresses); err != nil {
		return nil, err
	}

	finalOrder.ShippingAddresses = shippingAddresses

	return finalOrder, nil
}

func newOrder(o *order) *Order {
	return &Order{
		AdjustmentsTotal:       o.AdjustmentsTotal,
		Assignees:              o.Assignees,
		AssignmentCount:        o.AssignmentCount,
		BillingAddressesCount:  o.BillingAddressesCount,
		CreatedAt:              o.CreatedAt,
		CreditCardCount:        o.CreditCardCount,
		CreditCardTotal:        o.CreditCardTotal,
		Currency:               o.Currency,
		GiftCardCount:          o.GiftCardCount,
		GiftCardTotal:          o.GiftCardTotal,
		GrandTotal:             o.GrandTotal,
		ID:                     o.ID,
		LineItemCount:          o.LineItemCount,
		PlacedAt:               o.PlacedAt,
		ReferenceNumber:        o.ReferenceNumber,
		ReturnCount:            o.ReturnCount,
		Returns:                o.Returns,
		ShipmentCount:          o.ShipmentCount,
		ShippingAddressesCount: o.ShippingAddressesCount,
		ShippingTotal:          o.ShippingTotal,
		State:                  o.State,
		StoreCreditCount:       o.StoreCreditCount,
		StoreCreditTotal:       o.StoreCreditTotal,
		SubTotal:               o.SubTotal,
		TaxesTotal:             o.TaxesTotal,
	}
}

type order struct {
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
