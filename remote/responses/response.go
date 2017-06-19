package responses

// Response is a generic HTTP response type.
type Response struct {
	StatusCode int
	Body       interface{}
	Errs       []error
}
