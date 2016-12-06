package payloads

import (
	"strconv"

	"github.com/FoxComm/highlander/middlewarehouse/common/utils"
	"github.com/FoxComm/highlander/middlewarehouse/consumers/shipstation/phoenix"
)

// OrderItem is a single line item in an order.
type OrderItem struct {
	LineItemKey       string
	SKU               string `json:"sku"`
	Name              string
	ImageURL          string `json:"imageUrl"`
	Weight            Weight
	Quantity          int
	UnitPrice         float64
	TaxAmount         float64
	ShippingAmount    float64
	WarehouseLocation string
	Options           []ItemOption
	ProductID         *int
	FulfillmentSKU    *string `json:"fulfillmentSku"`
	Adjustment        bool
	UPC               *string `json:"upc"`
}

func NewOrderItemsFromPhoenix(items []phoenix.OrderLineItem) []OrderItem {
	condensedItems := make(map[uint]OrderItem)

	for _, item := range items {
		newItem := NewOrderItemFromPhoenix(item)
		ci, ok := condensedItems[item.SkuID]
		if ok {
			newItem.Quantity += ci.Quantity
		}
		condensedItems[item.SkuID] = newItem
	}

	orderItems := []OrderItem{}
	for _, orderItem := range condensedItems {
		orderItems = append(orderItems, orderItem)
	}

	return orderItems
}

func NewOrderItemFromPhoenix(item phoenix.OrderLineItem) OrderItem {
	return OrderItem{
		LineItemKey:    item.ReferenceNumber,
		FulfillmentSKU: utils.ToStringPtr(strconv.Itoa(int(item.SkuID))),
		SKU:            item.SkuCode,
		Name:           item.Name,
		UnitPrice:      float64(item.Price) / 100.0,
		Quantity:       1,
		Weight:         Weight{Value: 1, Units: "ounces"},
	}
}
