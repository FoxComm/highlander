package responses

// ProductTag represents the API response for a product tag.
type ProductTag struct {
	ID   int `json:"tagId"`
	Name string
}
