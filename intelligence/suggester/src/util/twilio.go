package util

import (
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
)

var (
	twilioAccountSid  string = os.Getenv("TWILIO_ACCOUNT_SID")
	twilioAuthToken   string = os.Getenv("TWILIO_AUTH_TOKEN")
	twilioPhoneNumber string = os.Getenv("TWILIO_PHONE_NUMBER")
)

func RetailPriceToUsdString(priceInCents string) (string, error) {
	priceInCentsFloat, priceInCentsErr := strconv.ParseFloat(priceInCents, 64)

	if priceInCentsErr != nil {
		return "", priceInCentsErr
	}

	usd := priceInCentsFloat / 100.0

	currencyFmtStr := fmt.Sprintf("$%.2f", usd)

	return currencyFmtStr, nil
}

func SuggestionToSMS(phoneNumber string, imageUrl string, product responses.ProductInstance) (responses.TwilioSmsResponse, error) {

	// Variables
	urlStr := "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json"
	retailPriceUSD, currencyErr := RetailPriceToUsdString(product.RetailPrice)
	if currencyErr != nil {
		return responses.TwilioSmsResponse{}, currencyErr
	}

	// Values
	v := url.Values{}
	v.Set("To", phoneNumber)
	v.Set("From", twilioPhoneNumber)
	v.Set("Body", product.Title+" for "+retailPriceUSD+". Reply \"yes\" to purchase or \"no\" to decline")
	v.Set("MediaUrl", imageUrl)

	rb := *strings.NewReader(v.Encode())

	// Client
	client := &http.Client{}

	req, _ := http.NewRequest("POST", urlStr, &rb)
	req.SetBasicAuth(twilioAccountSid, twilioAuthToken)
	req.Header.Add("Accept", "application/json")
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")

	// Request
	resp, reqErr := client.Do(req)
	if reqErr != nil {
		return responses.TwilioSmsResponse{}, reqErr
	}
	defer resp.Body.Close()

	// Response
	var twilioSmsResponse responses.TwilioSmsResponse
	jsonErr := json.NewDecoder(resp.Body).Decode(&twilioSmsResponse)
	if jsonErr != nil {
		return responses.TwilioSmsResponse{}, jsonErr
	}

	return twilioSmsResponse, nil
}
