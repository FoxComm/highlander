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
	return responses.Error{
		Errors: []string{f.err.Error()},
	}
}

func NewNotFound(err error) notFound {
	return notFound{err: err}
}
