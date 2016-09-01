package payloads

// TODO: Move to a consumer.
type Capture struct {
	ReferenceNumber string              `json:"order"`
	Items           []CaptureLineItem   `json:"items"`
	Shipping        CaptureShippingCost `json:"shipping"`
}

type CaptureLineItem struct {
	ReferenceNumber string `json:"ref"`
	SKU             string `json:"sku"`
}

type CaptureShippingCost struct {
	Total    int    `json:"total"`
	Currency string `json:"currency"`
}
