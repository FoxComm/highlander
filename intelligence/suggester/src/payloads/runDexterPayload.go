package payloads

type RunDexterProductData struct {
	Title    string `json:"title"`
	ImageURL string `json:"image_url"`
	Price    string `json:"price"`
}

type RunDexterPayload struct {
	Data []RunDexterProductData `json:"products"`
}
