package failures

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

func NewNotFound(exception exceptions.IException) failure {
	return newFailure(exception, http.StatusNotFound)
}

func NewBadRequest(exception exceptions.IException) failure {
	return newFailure(exception, http.StatusBadRequest)
}

func NewInternalError(exception exceptions.IException) failure {
	return newFailure(exception, http.StatusInternalServerError)
}
