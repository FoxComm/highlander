package responses

// Response is a generic HTTP response type.
type Response struct {
	StatusCode int
	Body       interface{}
	Errs       []error
}

func NewErrorResponse(err error) *Response {
	if err.Error() == "record not found" {
		return nil
	}

	return nil
}
