package searchrow

type SearchVariant struct {
	VariantID        int    `json:"variantId"`
	VariantName      string `json:"variantName"`
	VariantValueID   int    `json:"variantValueId"`
	VariantValueName string `json:"variantValueName"`
}
