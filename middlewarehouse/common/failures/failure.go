package failures

import (
	"github.com/gin-gonic/gin"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/errors"
)

type Failure interface {
	Status() int
	ToJSON() responses.Error
}

func Abort(context *gin.Context, failure Failure) {
	context.JSON(failure.Status(), failure.ToJSON())
	context.Abort()
}

func newFailure(error error, status int) failure {
	return failure{
		exception: error,
		status:    status,
	}
}

type failure struct {
	exception error
	status    int
}

func (failure failure) Status() int {
	return failure.status
}

func (failure failure) ToJSON() responses.Error {
	if exception, ok := failure.exception.(errors.AggregateError); ok {
		return responses.Error{
			Errors: exception.Messages(),
		}
	}

	return responses.Error{
		Errors: []string{failure.exception.Error()},
	}
}
