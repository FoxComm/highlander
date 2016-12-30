package payloads

type Money struct {
	Currency string `json:"currency"`
	Value    uint   `json:"value"`
}
