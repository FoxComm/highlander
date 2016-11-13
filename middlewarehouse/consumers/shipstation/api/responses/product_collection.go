package responses

// ProductCollection represents the response for a collection of products.
type ProductCollection struct {
	Products []Product
	Pagination
}
