package api

type Variant struct {
	Attributes map[string]ObjectAttribute `json:"attributes"`
	Values     []VariantValue             `json:"values"`
}
