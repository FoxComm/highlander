package controllers

import (
	"strconv"

	"github.com/FoxComm/highlander/remote/responses"
	"github.com/labstack/echo"
)

// FoxContext is a wrapper around echo.Context that eases error handling,
// provides helper methods, and ensures we have consistent response handling.
type FoxContext struct {
	echo.Context
	resp *responses.Response
}

// NewFoxContext creates a new FoxContext from an existing echo.Context.
func NewFoxContext(c echo.Context) *FoxContext {
	return &FoxContext{c, nil}
}

// ParamInt parses an integer from the parameters list (as defined by the URI).
func (fc *FoxContext) ParamInt(name string) int {
	if fc.resp != nil {
		return 0
	}

	param := fc.Param(name)
	if param == "" {
		fc.resp = errParamNotFound(name)
		return 0
	}

	paramInt, err := strconv.Atoi(param)
	if err != nil {
		fc.resp = errParamMustBeNumber(name)
		return 0
	}

	return paramInt
}

// ParamString parses an string from the parameters list (as defined by the URI).
func (fc *FoxContext) ParamString(name string) string {
	if fc.resp != nil {
		return ""
	}

	param := fc.Param(name)
	if param == "" {
		fc.resp = errParamNotFound(name)
		return ""
	}

	return param
}

// Run executes the primary controller method and returns the response.
func (fc *FoxContext) Run(ctrlFn ControllerFunc) error {
	if fc.resp != nil {
		return fc.handleResponse(fc.resp)
	}

	return fc.handleResponse(ctrlFn())
}

func (fc *FoxContext) handleResponse(resp *responses.Response) error {
	if len(resp.Errs) == 0 {
		return fc.JSON(resp.StatusCode, resp.Body)
	}

	errors := make([]string, len(resp.Errs))
	for i, err := range resp.Errs {
		errors[i] = err.Error()
	}

	errResp := map[string][]string{
		"errors": errors,
	}

	return fc.JSON(resp.StatusCode, errResp)
}
