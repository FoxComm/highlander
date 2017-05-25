package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"

	"bytes"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/shared/phoenix"
	"github.com/FoxComm/metamorphosis"
)

type OrderConsumer struct {
	phoenixClient phoenix.PhoenixClient
	apiURL        string
}

type Sku struct {
	ProductId   int    `json:"productFormId"`
	ImagePath   string `json:"imagePath"`
	SkuCode     string `json:"sku"`
	ProductName string `json:"name"`
}

type LineItems struct {
	Skus []Sku `json:"skus"`
}

type Customer struct {
	Id int `json:"id"`
}

type Order struct {
	Customer  Customer  `json:"customer"`
	LineItems LineItems `json:"lineItems"`
}

type Activity struct {
	Order Order `json:"order"`
}

type StringAttr struct {
	T string `json:"t"`
	V string `json:"v"`
}

type NewReviewAttributes struct {
	Status      StringAttr `json:"status"`
	ImgURL      StringAttr `json:"imageUrl"`
	ProductName StringAttr `json:"productName"`
}

type NewReviewPayload struct {
	SkuCode    string              `json:"sku"`
	CustID     int                 `json:"userId"`
	Attributes NewReviewAttributes `json:"attributes"`
}

func stringAttr(value string) StringAttr {
	return StringAttr{
		T: "string",
		V: value,
	}
}

func reviewAttrs(productName, imageURL string) NewReviewAttributes {
	return NewReviewAttributes{
		Status:      stringAttr("pending"),
		ImgURL:      stringAttr(imageURL),
		ProductName: stringAttr(productName),
	}
}

const (
	orderCheckoutCompleted = "order_checkout_completed"
)

func NewOrderConsumer(phoenixClient phoenix.PhoenixClient, apiURL string) (*OrderConsumer, error) {
	if err := phoenixClient.EnsureAuthentication(); err != nil {
		log.Panicf("Error auth in phoenix with error: %s", err.Error())
	}
	return &OrderConsumer{phoenixClient, apiURL}, nil
}

func (o OrderConsumer) parseData(data string) ([]NewReviewPayload, error) {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		log.Fatalf("Activity Parse Error %s", jsonErr.Error())
		return nil, jsonErr
	}

	skus := act.Order.LineItems.Skus
	payloads := make([]NewReviewPayload, 0)
	for i := 0; i < len(skus); i++ {
		payloads = append(payloads, NewReviewPayload{
			SkuCode:    skus[i].SkuCode,
			CustID:     act.Order.Customer.Id,
			Attributes: reviewAttrs(skus[i].ProductName, skus[i].ImagePath),
		})
	}

	return payloads, nil
}

func (o OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
		return err
	}

	if activity.Type() == orderCheckoutCompleted {
		payloads, err := o.parseData(activity.Data())
		if err != nil {
			log.Fatalf("Unable to parse json with error %s", err.Error())
		}

		for i := 0; i < len(payloads); i++ {
			b, err := json.Marshal(&payloads[i])
			if err != nil {
				return err
			}

			url := fmt.Sprintf("%s/v1/review/default", o.apiURL)
			req, err := http.NewRequest("POST", url, bytes.NewBuffer(b))
			if err != nil {
				return err
			}

			req.Header.Set("Content-Type", "application/json")
			req.Header.Set("JWT", o.phoenixClient.GetJwt())

			client := &http.Client{}
			resp, err := client.Do(req)
			if err != nil {
				log.Printf("Error creating review with error: %s", err.Error())
			}

			defer resp.Body.Close()
			if resp.StatusCode < 200 || resp.StatusCode > 299 {
				errResp, err := ioutil.ReadAll(resp.Body)
				if err != nil {
					return fmt.Errorf(
						"Failed to create review. Unable to read response with error %s",
						err.Error(),
					)
				}

				return fmt.Errorf(
					"Failed to create review with error %s",
					string(errResp),
				)
			}
			log.Printf("Review created for customer %d, sku %s", payloads[i].CustID, payloads[i].SkuCode)
		}
	}
	return nil
}
