package responses

// Money represents the payload for how we present money through the API, as a
// combination between an integer value (for amount) and the currency in which
// it is represented.
type Money struct {
	Currency string `json:"currency"`
	Value    uint   `json:"value"`
}
