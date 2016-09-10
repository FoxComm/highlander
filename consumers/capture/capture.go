package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
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

	b, err := json.Marshal(&capture)
	if err != nil {
		log.Printf("Error marshalling")
		return err
	}

	log.Printf("Payload: %s", string(b))

	url := fmt.Sprintf("%s/v1/service/capture", phoenixURL)
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
	if err != nil {
		log.Printf("Error creating post")
		return err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("JWT", phoenixJWT)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error on the request")
		return err
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		msg := fmt.Sprintf("Error in response from capture with status %d", resp.StatusCode)
		log.Printf(msg)
		return errors.New(msg)
	}

	return nil
}
