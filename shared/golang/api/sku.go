package api

type SKU struct {
	Attributes map[string]ObjectAttribute `json:"attributes"`
	Albums     []Album                    `json:"album"`
}
