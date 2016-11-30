package lib

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

type CapturePayload struct {
	ReferenceNumber string              `json:"order"`
	Items           []CaptureLineItem   `json:"items"`
	Shipping        CaptureShippingCost `json:"shipping"`
}

type CaptureLineItem struct {
	ReferenceNumber string `json:"ref"`
	SKU             string `json:"sku"`
}

type CaptureShippingCost struct {
	Total    int    `json:"total"`
	Currency string `json:"currency"`
}

func NewCapturePayload(activity activities.ISiteActivity) (*CapturePayload, error) {
	shipment := new(responses.Shipment)
	if err := json.Unmarshal([]byte(activity.Data()), shipment); err != nil {
		return nil, fmt.Errorf("Unable to marshal activity into shipment with %s", err.Error())
	}

	capture := CapturePayload{
		ReferenceNumber: shipment.OrderRefNum,
		Shipping: CaptureShippingCost{
			Total:    int(shipment.ShippingMethod.Cost),
			Currency: "USD",
		},
	}

	for _, lineItem := range shipment.ShipmentLineItems {
		for i := 0; i < len(lineItem.ReferenceNumbers); i++ {
			cLineItem := CaptureLineItem{
				ReferenceNumber: lineItem.ReferenceNumbers[i],
				SKU:             lineItem.SKU,
			}

			capture.Items = append(capture.Items, cLineItem)
		}

	}

	return &capture, nil
}
