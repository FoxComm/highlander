package api

type VariantValue struct {
	ID        int      `json:"id"`
	Name      string   `json:"name"`
	Swatch    string   `json:"swatch"`
	SKUCodes  []string `json:"skuCodes"`
	VariantID int
}
