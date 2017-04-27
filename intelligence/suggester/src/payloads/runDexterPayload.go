package payloads

type RunDexterProductData struct {
	ImageURL string `json:"image_url"`
	Price    string `json:"price"`
}

type RunDexterPayload struct {
	Data []RunDexterProductData `json:"products"`
}
