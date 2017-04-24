package util

import (
	"os"

	"github.com/FoxComm/highlander/intelligence/suggester/src/payloads"
	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"

	"github.com/FoxComm/highlander/middlewarehouse/lib/gohttp"
)

var (
	runDexterBotID   string = os.Getenv("DEXTER_BOT_ID")
	runDexterApiKey  string = os.Getenv("DEXTER_API_KEY")
	runDexterBaseURL string = os.Getenv("DEXTER_BASE_URL")
)

func DexterSuggestionToSMS(phoneNumber string, command string, product responses.ProductInstance) (responses.RunDexterResponse, error) {
	// Set endpoint url
	dexterPostApiEndpoint := runDexterBaseURL + runDexterBotID + "/command/" + command + "/platform/twilio/" + phoneNumber + "/?api_key=" + runDexterApiKey

	// Extract product data
	productImageURL := product.Albums[0].Images[0].Src

	retailPriceUSD, currencyErr := RetailPriceToUsdString(product.RetailPrice)
	if currencyErr != nil {
		return responses.RunDexterResponse{}, currencyErr
	}

	// Build payload
	productData := payloads.RunDexterProductData{
		Message:  "We've got a great product recommendation for you...",
		ImageURL: productImageURL,
		Price:    retailPriceUSD,
	}
	dataPayload := payloads.RunDexterPayload{
		Data: []payloads.RunDexterProductData{productData},
	}

	// HTTP POST Request
	postHeaders := map[string]string{}
	var runDexterResp responses.RunDexterResponse
	postErr := gohttp.Post(dexterPostApiEndpoint, postHeaders, &dataPayload, &runDexterResp)

	if postErr != nil {
		return responses.RunDexterResponse{}, postErr
	}

	return runDexterResp, nil
}
