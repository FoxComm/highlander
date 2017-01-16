package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type internalError struct {
	err error
}

func (failure internalError) Status() int {
	return http.StatusInternalServerError
}

func (failure internalError) ToJSON() responses.Error {
	return toJSON(failure.err)
}

func NewInternalError(err error) internalError {
	return internalError{err: err}
}
