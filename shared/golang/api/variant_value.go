package api

type VariantValue struct {
	ID        int      `json:"id"`
	Name      string   `json:"name"`
	SKUCodes  []string `json:"skuCodes"`
	VariantID int
}
