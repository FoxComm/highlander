package controllers

import (
	"errors"
	"fmt"
	"net/http"
	"strconv"

	"github.com/FoxComm/highlander/remote/utils/failures"
	"github.com/labstack/echo"
)

// FoxContext is a wrapper around echo.Context that eases error handling,
// provides helper methods, and ensures we have consistent response handling.
type FoxContext struct {
	echo.Context
	failure failures.Failure
}

// NewFoxContext creates a new FoxContext from an existing echo.Context.
func NewFoxContext(c echo.Context) *FoxContext {
	return &FoxContext{c, nil}
}

// ParamInt parses an integer from the parameters list (as defined by the URI).
func (fc *FoxContext) ParamInt(name string) int {
	if fc.failure != nil {
		return 0
	}

	param := fc.Param(name)
	if param == "" {
		fc.failure = failures.NewParamNotFound(name)
		return 0
	}

	paramInt, err := strconv.Atoi(param)
	if err != nil {
		fc.failure = failures.NewParamInvalidType(name, "number")
		return 0
	}

	return paramInt
}

// ParamString parses an string from the parameters list (as defined by the URI).
func (fc *FoxContext) ParamString(name string) string {
	if fc.failure != nil {
		return ""
	}

	param := fc.Param(name)
	if param == "" {
		fc.failure = failures.NewParamNotFound(name)
		return ""
	}

	return param
}

// Run executes the primary controller method and returns the response.
func (fc *FoxContext) Run(ctrlFn ControllerFunc) error {
	if fc.failure != nil {
		return fc.handleFailure(fc.failure)
	}

	resp, failure := ctrlFn()
	if failure != nil {
		return fc.handleFailure(failure)
	}

	return fc.JSON(resp.StatusCode, resp.Body)
}

func (fc *FoxContext) handleFailure(failure failures.Failure) error {
	if failure == nil {
		return errors.New("handleFailure must receive a failure")
	} else if !failure.HasError() {
		return errors.New("handleFailure must receive a failure with an error")
	}

	if err := failure.Log(); err != nil {
		return fmt.Errorf("Error trying to log failure with error: %s", err.Error())
	}

	errString := failure.Error()
	failureType, err := failure.Type()
	if err != nil {
		return err
	}

	var statusCode int
	switch failureType {
	case failures.FailureBadRequest:
		statusCode = http.StatusBadRequest
	case failures.FailureNotFound:
		statusCode = http.StatusNotFound
	case failures.FailureServiceError:
		statusCode = http.StatusInternalServerError
		errString = "Unexpected error occurred"
	default:
		return fmt.Errorf("Invalid failure type, got %d", failureType)
	}

	errResp := map[string][]string{
		"errors": []string{errString},
	}

	return fc.JSON(statusCode, errResp)
}
