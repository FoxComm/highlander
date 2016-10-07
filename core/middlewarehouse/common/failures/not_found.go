package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

type notFound struct {
	err error
}

func (f notFound) Status() int {
	return http.StatusNotFound
}

func (f notFound) ToJSON() responses.Error {
	return toJSON(f.err)
}

func NewNotFound(err error) notFound {
	return notFound{err: err}
}
