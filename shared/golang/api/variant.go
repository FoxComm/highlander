package api

type Variant struct {
	Attributes ObjectAttributes `json:"attributes"`
	Values     []VariantValue   `json:"values"`
}

func (v Variant) Name() (string, error) {
	value, err := v.Attributes.LookupValue("name", "string")
	return value.(string), err
}

func (v Variant) Type() (string, error) {
	value, err := v.Attributes.LookupValue("type", "string")
	return value.(string), err
}
