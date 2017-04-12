package util

import (
	"encoding/json"
	"net/http"
	"net/url"
	"os"
	"strings"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
)

var twilioAccountSid = os.Getenv("TWILIO_ACCOUNT_SID")
var twilioAuthToken = os.Getenv("TWILIO_AUTH_TOKEN")
var twilioPhoneNumber = os.Getenv("TWILIO_PHONE_NUMBER")

func SuggestionToSMS(phoneNumber string, imageUrl string) (responses.TwilioSmsResponse, error) {

	// Variables
	urlStr := "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json"

	// Values
	v := url.Values{}
	v.Set("To", phoneNumber)
	v.Set("From", twilioPhoneNumber)
	v.Set("Body", "Hello, from FoxCommerce! Text \"buy\" if you want to buy this product, or \"pass\" to decline")
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