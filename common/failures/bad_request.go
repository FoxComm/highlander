package failures

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

type BadRequest struct {
	err error
}

func (f BadRequest) Status() int {
	return http.StatusBadRequest
}

func (f BadRequest) ToJSON() responses.Error {
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func MakeBadRequest(err error) BadRequest {
	return BadRequest{err: err}
}
