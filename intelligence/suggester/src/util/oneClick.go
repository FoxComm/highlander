package util

import (
	"log"

	"fmt"

	"github.com/FoxComm/highlander/intelligence/suggester/src/suggester_phoenix"
	"github.com/FoxComm/highlander/middlewarehouse/lib/gohttp"
	"github.com/FoxComm/highlander/middlewarehouse/shared"
)

type OneClickProducts struct {
	Items []OneClickProduct `json:"items"`
}

type OneClickProduct struct {
	SKU      string `json:"sku"`
	Quantity int    `json:"quantity"`
}

func makePostRequestPayload(productSKU string) OneClickProducts {
	oneClickProduct := OneClickProduct{SKU: productSKU, Quantity: 1}
	oneClickProducts := OneClickProducts{Items: []OneClickProduct{oneClickProduct}}
	return oneClickProducts
}

func OneClickPurchase(customerID string, productSKU string) (string, error) {
	phoenixConfig, phoenixConfigErr := shared.MakePhoenixConfig()
	if phoenixConfigErr != nil {
		return "", phoenixConfigErr
	}

	phoenixClient := suggester_phoenix.NewPhoenixClient(phoenixConfig.URL, phoenixConfig.User, phoenixConfig.Password)
	if phxAuthErr := phoenixClient.Authenticate(); phxAuthErr != nil {
		return "", phxAuthErr
	}

	if ensureAuthErr := phoenixClient.EnsureAuthentication(); ensureAuthErr != nil {
		return "", ensureAuthErr
	}

	action := "/v1/customers/" + customerID + "/checkout"
	payload := makePostRequestPayload(productSKU)
	endpoint := fmt.Sprintf("%s%s", phoenixClient.GetBaseURL(), action)
	captureResp := new(map[string]interface{})

	postErr := gohttp.Post(endpoint, phoenixClient.GetDefaultHeaders(), &payload, captureResp)
	if postErr != nil {
		return "", postErr
	}

	responseMessage := fmt.Sprintf("Successfully captured from Phoenix with response: %v", captureResp)
	log.Printf(responseMessage)

	return responseMessage, nil
}
