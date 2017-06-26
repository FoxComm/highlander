package responses

type SkuAfs struct {
	Sellable    int `json:"sellable"`
	NonSellable int `json:"nonSellable"`
	Backorder   int `json:"backorder"`
	Preorder    int `json:"preorder"`
}
