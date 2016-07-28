package failures

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
)

type internalError struct {
	err error
}

func (f internalError) Status() int {
	return http.StatusInternalServerError
}

func (f internalError) ToJSON() responses.Error {
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func NewInternalError(err error) internalError {
	return internalError{err: err}
}
