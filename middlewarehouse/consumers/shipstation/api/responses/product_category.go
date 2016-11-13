package responses

// ProductCategory represents the API response for a product category.
type ProductCategory struct {
	ID   int `json:"categoryId"`
	Name string
}
