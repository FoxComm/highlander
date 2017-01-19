package fixtures

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/icrowley/fake"
)

func GetOrder(refNum string, lineItemCount int) *payloads.Order {
	skus := []payloads.OrderLineItem{}
	for i := 0; i < lineItemCount; i++ {
		sku := payloads.OrderLineItem{
			SKU:              fake.CharactersN(10),
			Name:             fake.ProductName(),
			Price:            uint(randomInt()),
			State:            "pending",
			ReferenceNumbers: []string{fake.CharactersN(10)},
			ImagePath:        "test.com/test.png",
		}

		skus = append(skus, sku)
	}

	order := &payloads.Order{
		Totals: payloads.OrderTotals{
			Shipping:    0,
			Adjustments: 0,
			SubTotal:    1000,
			Taxes:       60,
			Total:       1060,
		},
		Customer: payloads.Customer{
			ID:    1,
			Name:  "Don Quixote",
			Email: "don@quixote.com",
		},
		LineItems:       payloads.OrderLineItems{SKUs: skus},
		OrderState:      "fulfillmentStarted",
		PaymentState:    "auth",
		ShippingState:   "pending",
		ReferenceNumber: refNum,
		ShippingMethod:  nil,
		ShippingAddress: &payloads.Address{
			ID:   1,
			Name: "Don Quixote",
			Region: payloads.Region{
				ID:          4177,
				Name:        "Washington",
				CountryID:   234,
				CountryName: "United States",
			},
		},
	}

	order.Scope = "1.2"

	return order
}
