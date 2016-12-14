package payloads

import "github.com/FoxComm/highlander/middlewarehouse/models"

// Order represents the order object that exists in the orders_search_view.
type Order struct {
	Totals          OrderTotals         `json:"totals" binding:"required"`
	Customer        Customer            `json:"customer" binding:"required"`
	PlacedAt        string              `json:"placedAt" binding:"required"`
	LineItems       OrderLineItems      `json:"lineItems" binding:"required"`
	FraudScore      int                 `json:"fraudScore"`
	OrderState      string              `json:"orderState" binding:"required"`
	PaymentState    string              `json:"paymentState" binding:"required"`
	ShippingState   string              `json:"shippingState" binding:"required"`
	PaymentMethods  []PaymentMethod     `json:"paymentMethods" binding:"required"`
	ShippingMethod  OrderShippingMethod `json:"shippingMethod" binding:"required"`
	ReferenceNumber string              `json:"referenceNumber" binding:"required"`
	ShippingAddress Address             `json:"shippingAddress" binding:"required"`
	RemorseHoldEnd  *string             `json:"remorseHoldEnd"`
	Scopable
}

func (order *Order) SetScope(scope string) {
	order.Scope = scope

	order.ShippingMethod.SetScope(scope)
}

// Order wrapped in Phoenix response
type OrderResult struct {
	Order Order `json:"result" binding:"required"`
}

func (payload *Order) ShipmentModel() *models.Shipment {
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

	return shipment
}
