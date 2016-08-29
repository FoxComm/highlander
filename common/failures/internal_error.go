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
	return toJSON(f.err)
}

func NewInternalError(err error) internalError {
	return internalError{err: err}
}
