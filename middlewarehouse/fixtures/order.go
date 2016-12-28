package fixtures

import "github.com/FoxComm/highlander/middlewarehouse/api/payloads"

func GetOrder() *payloads.Order {
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
		LineItems: payloads.OrderLineItems{
			SKUs: []payloads.OrderLineItem{
				payloads.OrderLineItem{
					SKU:              "TEST-SKU",
					Name:             "Some test SKU",
					Price:            999,
					State:            "pending",
					ReferenceNumbers: []string{"REFNUM"},
					ImagePath:        "test.com/test.png",
				},
			},
		},
		OrderState:      "cart",
		PaymentState:    "cart",
		ShippingState:   "cart",
		ReferenceNumber: "BR10004",
		ShippingMethod:  nil,
	}

	order.Scope = "1.2"

	return order
}
