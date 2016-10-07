package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type badRequest struct {
	err error
}

func (f badRequest) Status() int {
	return http.StatusBadRequest
}

func (f badRequest) ToJSON() responses.Error {
	return toJSON(f.err)
}

func NewBadRequest(err error) badRequest {
	return badRequest{err: err}
}
