package api

import "fmt"

type Variant struct {
	ID         int              `json:"id"`
	Attributes ObjectAttributes `json:"attributes"`
	Values     []VariantValue   `json:"values"`
}
type Variants []Variant

func (vv Variants) FindByName(name string) (*Variant, error) {
	for _, v := range vv {
		variantName, err := v.Name()
		if err != nil {
			continue
		}
		if name == variantName {
			return &v, nil
		}
	}
	return nil, fmt.Errorf("Variant with name %s not found", name)
}

func (v Variant) Name() (string, error) {
	value, err := v.Attributes.LookupValue("name")
	return value.(string), err
}

func (v Variant) Type() (string, error) {
	value, err := v.Attributes.LookupValue("type")
	return value.(string), err
}
