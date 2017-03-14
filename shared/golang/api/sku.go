package api

import "errors"

type SKU struct {
	ID         int              `json:"id"`
	Context    Context          `json:"context"`
	Attributes ObjectAttributes `json:"attributes"`
	Albums     []Album          `json:"albums"`
}

func (s SKU) Code() (string, error) {
	value, err := s.Attributes.LookupValue("code")
	return value.(string), err
}

func (s SKU) Title() (string, error) {
	value, _ := s.Attributes.LookupValue("title")
	return value.(string), nil
}

func (s SKU) SalePrice() (*ObjectPrice, error) {
	value, ok := s.Attributes["salePrice"]
	if !ok {
		return nil, errors.New("Attribute salePrice not found")
	}

	price, err := NewObjectPrice(value)
	if err != nil {
		return nil, err
	}

	return price, nil
}

func (s SKU) FirstImage() string {
	if len(s.Albums) == 0 {
		return ""
	}

	if len(s.Albums[0].Images) == 0 {
		return ""
	}

	return s.Albums[0].Images[0].Source
}
