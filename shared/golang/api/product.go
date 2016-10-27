package api

type Product struct {
	ID         int                        `json:"id"`
	Context    Context                    `json:"context"`
	Attributes map[string]ObjectAttribute `json:"attributes"`
	Variants   []Variant                  `json:"variants"`
	SKUs       []SKU                      `json:"skus"`
	Albums     []Album                    `json:"albums"`
}
