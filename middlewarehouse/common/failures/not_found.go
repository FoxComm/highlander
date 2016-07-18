package failures

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

type NotFound struct {
	err error
}

func (f NotFound) Status() int {
	return http.StatusNotFound
}

func (f NotFound) ToJSON() responses.Error {
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func MakeNotFound(err error) NotFound {
	return NotFound{err: err}
}
