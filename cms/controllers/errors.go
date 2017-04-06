package controllers

import (
	"errors"
	"fmt"

	"github.com/FoxComm/highlander/cms/common"
)

const (
	emptyRoutePrefixErrorMsg     = "Must specify a non-empty route prefix"
	beginningRoutePrefixErrorMsg = "Route prefix '%s' must begin with '/'"
	endingRoutePrefixErrorMsg    = "Route prefix '%s' must end with '/'"

	paramIsRequiredErrorMsg = "URL parameter '%s' is required"
)

func newEmptyRoutePrefixError() error {
	return errors.New(emptyRoutePrefixErrorMsg)
}

func newBeginningRoutePrefixError(prefix string) error {
	return fmt.Errorf(beginningRoutePrefixErrorMsg, prefix)
}

func newEndingRoutePrefixError(prefix string) error {
	return fmt.Errorf(endingRoutePrefixErrorMsg, prefix)
}

func newParamIsRequiredError(name string) FoxError {
	err := fmt.Errorf(paramIsRequiredErrorMsg, name)
	return common.NewBadRequestError(err)
}
