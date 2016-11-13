package responses

// Pagination is a response type that handles the common format that we should
// expect for responses of collections related to pagination.
type Pagination struct {
	Total int
	Page  int
	Pages int
}
