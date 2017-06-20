package failures

import (
	"errors"
	"fmt"
)

func NewParamNotFound(paramName string) Failure {
	return &generalFailure{
		err:         fmt.Errorf("param %s not found", paramName),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewParamInvalidType(paramName string, expectedType string) Failure {
	return &generalFailure{
		err:         fmt.Errorf("param %s must be a %s", paramName, expectedType),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewBindFailure(err error) Failure {
	return &generalFailure{
		err:         fmt.Errorf("failed to parse then payload with error %s", err.Error()),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewEmptyPayloadFailure() Failure {
	return &generalFailure{
		err:         errors.New("payload must have contents"),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewFieldEmptyFailure(paramName string) Failure {
	return &generalFailure{
		err:         fmt.Errorf("%s must be non-empty", paramName),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewFieldGreaterThanZero(paramName string, value int) Failure {
	return &generalFailure{
		err:         fmt.Errorf("Expected %s to be greater than 0, got %d", paramName, value),
		failureType: FailureBadRequest,
		stack:       newCallStack(),
	}
}

func NewModelNotFoundFailure(modelName string, id int) Failure {
	return &generalFailure{
		err:         fmt.Errorf("%s with id %d was not found", modelName, id),
		failureType: FailureNotFound,
		stack:       newCallStack(),
	}
}
