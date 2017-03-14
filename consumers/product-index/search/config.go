package search

type Index struct {
	Id   int    `json:"id"`
	Name string `json:"name"`
}

const ProductField = "productField"
const OptionField = "optionField"

type Field struct {
	Id       int    `json:"id"`
	Name     string `json:"name"`
	Type     string `json:"type"`
	Analyzer string `json:"analyzer"`
}

type Config struct {
	Index          Index   `json:"index"`
	Attributes     []Field `json:"attributes"`
	VisualVariants []string
}
