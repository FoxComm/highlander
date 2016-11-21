package failures

import (
	"net/http"
)

func NewNotFound(error error) failure {
	return newFailure(error, http.StatusNotFound)
}

func NewBadRequest(error error) failure {
	return newFailure(error, http.StatusBadRequest)
}

func NewInternalError(error error) failure {
	return newFailure(error, http.StatusInternalServerError)
}
