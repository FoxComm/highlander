package payloads

// ProductTag represents the API payload for a product tag.
type ProductTag struct {
	ID   int `json:"tagId"`
	Name string
}
