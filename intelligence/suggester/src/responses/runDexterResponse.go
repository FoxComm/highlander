package responses

import (
	"time"
)

type SubResourceUri struct {
	Media string `json:"media"`
}

type SubResource_Uri struct {
	Media string `json:"media"`
}

type RunDexterData struct {
	Sid                   string          `json:"sid"`
	DateCreated           string          `json:"date_created"`
	DateUpdated           string          `json:"date_updated"`
	DateSent              interface{}     `json:"date_sent"`
	AccountSid            string          `json:"account_sid"`
	To                    string          `json:"to"`
	From                  string          `json:"from"`
	Messaging_Service_Sid interface{}     `json:"messaging_service_sid"`
	Body                  string          `json:"body"`
	Status                string          `json:"status"`
	Num_Segments          string          `json:"num_segments"`
	Num_Media             string          `json:"num_media"`
	Direction             string          `json:"direction"`
	API_Version           string          `json:"api_version"`
	Price                 interface{}     `json:"price"`
	Price_Unit            string          `json:"price_unit"`
	Error_Code            interface{}     `json:"error_code"`
	Error_Message         interface{}     `json:"error_message"`
	URI                   string          `json:"uri"`
	Subresource_Uris      SubResource_Uri `json:"subresource_uris"`
	DateCreatedUTC        time.Time       `json:"dateCreated"`
	DateUpdatedUTC        time.Time       `json:"dateUpdated"`
	DateSentUTC           time.Time       `json:"dateSent"`
	MessagingServiceSid   interface{}     `json:"messagingServiceSid"`
	NumSegments           string          `json:"numSegments"`
	NumMedia              string          `json:"numMedia"`
	APIVersion            string          `json:"apiVersion"`
	PriceUnit             string          `json:"priceUnit"`
	ErrorCode             interface{}     `json:"errorCode"`
	ErrorMessage          interface{}     `json:"errorMessage"`
	SubresourceUris       SubresourceUri  `json:"subresourceUris"`
}

type RunDexterResponse struct {
	Success bool            `json:"success"`
	Data    []RunDexterData `json:"data"`
}
