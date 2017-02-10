package payloads

import (
	"errors"

	"github.com/FoxComm/highlander/middlewarehouse/models"
)

// Order represents the order object that exists in the orders_search_view.
type Order struct {
	Totals          OrderTotals          `json:"totals"`
	Customer        Customer             `json:"customer"`
	PlacedAt        string               `json:"placedAt"`
	LineItems       OrderLineItems       `json:"lineItems"`
	FraudScore      int                  `json:"fraudScore"`
	OrderState      string               `json:"orderState"`
	PaymentState    string               `json:"paymentState"`
	ShippingState   string               `json:"shippingState"`
	PaymentMethods  []PaymentMethod      `json:"paymentMethods"`
	ShippingMethod  *OrderShippingMethod `json:"shippingMethod"`
	ReferenceNumber string               `json:"referenceNumber"`
	ShippingAddress *Address             `json:"shippingAddress"`
	RemorseHoldEnd  *string              `json:"remorseHoldEnd"`
	Scopable
}

func (order *Order) SetScope(scope string) {
	order.Scope = scope

	if order.ShippingMethod != nil {
		order.ShippingMethod.SetScope(scope)
	}
}

// Order wrapped in Phoenix response
type OrderResult struct {
	Order Order `json:"result" binding:"required"`
}

func (payload *Order) ShipmentModel() (*models.Shipment, error) {
	if payload.ShippingAddress == nil {
		return nil, errors.New("Order must contain shipping address for a shipment to be created")
	}

	shipment := &models.Shipment{
		ShippingMethodCode: payload.ShippingMethod.Code,
		OrderRefNum:        payload.ReferenceNumber,
		State:              models.ShipmentStatePending,
		Address:            *(payload.ShippingAddress.Model()),
		ShippingPrice:      payload.ShippingMethod.Price,
		Scope:              payload.Scope,
	}

	for _, lineItem := range payload.LineItems.SKUs {
		for i := 0; i < lineItem.Quantity; i++ {
			shipment.ShipmentLineItems = append(shipment.ShipmentLineItems, *(lineItem.Model()))
		}
	}

	return shipment, nil
}
