package failures

import "fmt"

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
