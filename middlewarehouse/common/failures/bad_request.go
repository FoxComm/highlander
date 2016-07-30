package failures

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

type badRequest struct {
	err error
}

func (f badRequest) Status() int {
	return http.StatusBadRequest
}

func (f badRequest) ToJSON() responses.Error {
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func NewBadRequest(err error) badRequest {
	return badRequest{err: err}
}
