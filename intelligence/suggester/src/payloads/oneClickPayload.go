package payloads

type OneClickProducts struct {
	Items []OneClickProduct `json:"items"`
}

type OneClickProduct struct {
	SKU      string `json:"sku"`
	Quantity int    `json:"quantity"`
}
