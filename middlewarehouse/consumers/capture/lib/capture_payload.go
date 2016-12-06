package lib

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
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
	SkuID           uint   `json:"skuId"`
	SkuCode         string `json:"skuCode"`
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
		cLineItem := CaptureLineItem{
			ReferenceNumber: lineItem.ReferenceNumber,
			SkuID:           lineItem.SkuID,
			SkuCode:         lineItem.SkuCode,
		}

		capture.Items = append(capture.Items, cLineItem)
	}

	return &capture, nil
}

func NewGiftCardCapturePayload(referenceNumber string, lineItems []payloads.OrderLineItem) (*CapturePayload, error) {
	capture := CapturePayload{
		ReferenceNumber: referenceNumber,
		Shipping: CaptureShippingCost{
			Total:    0,
			Currency: "USD",
		},
		Items: make([]CaptureLineItem, 0),
	}

	for _, lineItem := range lineItems {
		for i := 0; i < lineItem.Quantity; i++ {
			cLineItem := CaptureLineItem{
				SkuID:   lineItem.SkuID,
				SkuCode: lineItem.SkuCode,
			}
			capture.Items = append(capture.Items, cLineItem)
		}
	}

	return &capture, nil
}
