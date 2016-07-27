package failures

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

type InternalError struct {
	err error
}

func (f InternalError) Status() int {
	return http.StatusInternalServerError
}

func (f InternalError) ToJSON() responses.Error {
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func MakeInternalError(err error) InternalError {
	return InternalError{err: err}
}
