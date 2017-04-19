package responses

type SubresourceUri struct {
	Media string `json:"media"`
}

type TwilioSmsResponse struct {
	Sid             string         `json:"sid"`
	DateCreated     string         `json:"date_created"`
	DateUpdated     string         `json:"date_updated"`
	DateSent        string         `json:"date_sent"`
	AccountSid      string         `json:"account_sid"`
	To              string         `json:"to"`
	From            string         `json:"from"`
	Body            string         `json:"body"`
	NumSegments     string         `json:"num_segments"`
	NumMedia        string         `json:"num_media"`
	Direction       string         `json:"direction"`
	APIVersion      string         `json:"api_version"`
	Price           string         `json:"price"`
	PriceUnit       string         `json:"price_unit"`
	ErrorCode       string         `json:"error_code"`
	ErrorMessage    string         `json:"error_message"`
	URI             string         `json:"uri"`
	SubresourceUris SubresourceUri `json:"subresource_uris"`
}
