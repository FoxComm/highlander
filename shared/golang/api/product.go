package api

type Product struct {
	ID         int              `json:"id"`
	Context    Context          `json:"context"`
	Attributes ObjectAttributes `json:"attributes"`
	Variants   []Variant        `json:"variants"`
	SKUs       []SKU            `json:"skus"`
	Albums     []Album          `json:"albums"`
}

func (p Product) Title() (string, error) {
	value, err := p.Attributes.LookupValue("title")
	return value.(string), err
}

func (p Product) Description() string {
	value, _ := p.Attributes.LookupValue("description")
	return value.(string)
}

func (p Product) FirstImage() string {
	if len(p.Albums) == 0 {
		return ""
	}

	if len(p.Albums[0].Images) == 0 {
		return ""
	}

	return p.Albums[0].Images[0].Source
}

func (p Product) Tags() interface{} {
	value, _ := p.Attributes.LookupValue("tags")
	return value
}
