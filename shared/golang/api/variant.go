package api

type Variant struct {
	ID         int              `json:"id"`
	Attributes ObjectAttributes `json:"attributes"`
	Values     []VariantValue   `json:"values"`
}

func (v Variant) Name() (string, error) {
	value, err := v.Attributes.LookupValue("name")
	return value.(string), err
}

func (v Variant) Type() (string, error) {
	value, err := v.Attributes.LookupValue("type")
	return value.(string), err
}
