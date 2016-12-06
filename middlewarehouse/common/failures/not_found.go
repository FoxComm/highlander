package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type notFound struct {
	err error
}

func (failure notFound) Status() int {
	return http.StatusNotFound
}

func (failure notFound) ToJSON() responses.Error {
	return toJSON(failure.err)
}

func NewNotFound(err error) notFound {
	return notFound{err: err}
}
