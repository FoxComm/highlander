package failures

import (
	"errors"
	"fmt"
)

const (
	FailureBadRequest = iota
	FailureNotFound
	FailureServiceError

	recordNotFound = "record not found"
)

// Failure is a wrapper around the standard Golang error type that gives us more
// control for how to handle, log, and return it to the user. In addition to
// storing the root error that was thrown by calling code, it can provide a
// stack trace and logging information, should it be asked. Finally, each
// implementation of Failure provides a type, so that responses can be
// effectively returned to calling code.
type Failure interface {
	Error() string
	HasError() bool
	Trace() (string, error)
	Type() (int, error)
}

// New creates a new Failure. It determines the best failure based on the error
// and arguments passed in. If no error occurred, the response will be nil.
func New(err error, params map[string]interface{}) Failure {
	if err == nil {
		return nil
	}

	stack := newCallStack()

	if err.Error() == recordNotFound {
		return newNotFoundFailure(stack, err, params)
	}

	return newServiceFailure(stack, err)
}

func newNotFoundFailure(stack *callStack, originalErr error, params map[string]interface{}) Failure {
	model, modelOk := params["model"]
	id, idOk := params["id"]

	var notFoundErr error
	if !modelOk || !idOk {
		notFoundErr = originalErr
	} else {
		notFoundErr = fmt.Errorf("%s with id %d was not found", model, id)
	}

	return &generalFailure{
		err:         notFoundErr,
		failureType: FailureNotFound,
		stack:       stack,
	}
}

func newServiceFailure(stack *callStack, err error) Failure {
	return &generalFailure{
		err:         err,
		failureType: FailureServiceError,
		stack:       stack,
	}
}

// NewGeneralFailure creates a Failure with all basic parameters coming from the
// caller. No assumptions about message format are made by the system.
func NewGeneralFailure(err error, failureType int) Failure {
	return &generalFailure{
		err:         err,
		failureType: failureType,
		stack:       newCallStack(),
	}
}

type generalFailure struct {
	err         error
	failureType int
	stack       *callStack
}

func (f generalFailure) Error() string {
	if f.err != nil {
		return f.err.Error()
	}

	return ""
}

func (f generalFailure) HasError() bool {
	return f.err != nil
}

func (f generalFailure) Trace() (string, error) {
	if f.err == nil {
		return "", errors.New("Unable to get trace - no error occurred")
	}

	return fmt.Sprintf("%s, trace:\n%s", f.err.Error(), f.stack.stackTrace()), nil
}

func (f generalFailure) Type() (int, error) {
	if f.err == nil {
		return 0, errors.New("No error has occurred")
	}

	return f.failureType, nil
}
