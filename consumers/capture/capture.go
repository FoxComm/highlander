package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/consumers"
	"github.com/FoxComm/middlewarehouse/models/activities"
)

func CapturePayment(act activities.ISiteActivity, phoenixURL string, phoenixJWT string) error {
	shipment := new(responses.Shipment)
	if err := json.Unmarshal([]byte(act.Data()), shipment); err != nil {
		return err
	}

	log.Printf("Starting capture for %s", shipment.ReferenceNumber)
	capture := payloads.Capture{
		ReferenceNumber: shipment.ReferenceNumber,
		Shipping: payloads.CaptureShippingCost{
			Total:    0,
			Currency: "USD",
		},
	}

	for _, lineItem := range shipment.ShipmentLineItems {
		cLineItem := payloads.CaptureLineItem{
			ReferenceNumber: lineItem.ReferenceNumber,
			SKU:             lineItem.SKU,
		}

		capture.Items = append(capture.Items, cLineItem)
	}

	url := fmt.Sprintf("%s/v1/service/capture", phoenixURL)
	headers := map[string]string{
		"JWT": phoenixJWT,
	}

	_, err := consumers.Post(url, headers, &capture)
	if err != nil {
		log.Printf(err.Error())
		return err
	}

	return nil
}
