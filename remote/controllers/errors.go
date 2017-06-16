package controllers

import (
	"fmt"
	"net/http"

	"github.com/FoxComm/highlander/remote/responses"
)

func errParamMustBeNumber(paramName string) *responses.Response {
	return &responses.Response{
		StatusCode: http.StatusBadRequest,
		Errs: []error{
			fmt.Errorf("Param %s must be a number", paramName),
		},
	}
}

func errParamNotFound(paramName string) *responses.Response {
	return &responses.Response{
		StatusCode: http.StatusBadRequest,
		Errs: []error{
			fmt.Errorf("Param %s not found", paramName),
		},
	}
}
