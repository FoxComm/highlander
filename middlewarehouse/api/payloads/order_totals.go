package payloads

type OrderTotals struct {
	Taxes       int `json:"taxes"`
	Total       int `json:"total"`
	Shipping    int `json:"shipping"`
	SubTotal    int `json:"subTotal"`
	Adjustments int `json:"adjustments"`
	Scopable
}
