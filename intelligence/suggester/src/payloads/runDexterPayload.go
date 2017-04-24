package payloads

type RunDexterProductData struct {
	Message  string `json:"message"`
	ImageURL string `json:"image_url"`
	Price    string `json:"price"`
}

type RunDexterPayload struct {
	Data []RunDexterProductData `json:"products"`
}
