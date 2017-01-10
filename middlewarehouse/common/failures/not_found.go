package failures

import (
	"fmt"
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
)

const notFoundErrorMessage = "%s with id=%d not found"

type notFound struct {
	err error
}

func (failure notFound) Error() string {
	return failure.err.Error()
}

func (failure notFound) Status() int {
	return http.StatusNotFound
}

func (failure notFound) ToJSON() responses.Error {
	return toJSON(failure.err)
}

// NewNotFound creates a new Not Found Failure.
func NewNotFound(objectName string, id uint) Failure {
	return &notFound{
		err: fmt.Errorf(notFoundErrorMessage, objectName, id),
	}
}
