package responses

// Response is a generic HTTP response type.
type Response struct {
	StatusCode int
	Body       interface{}
}

// NewResponse creates a new response object.
func NewResponse(statusCode int, body interface{}) *Response {
	return &Response{
		StatusCode: statusCode,
		Body:       body,
	}
}
