package failures

import (
	"github.com/gin-gonic/gin"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

type Failure interface {
	Status() int
	ToJSON() responses.Error
}

func Abort(context *gin.Context, failure Failure) {
	context.JSON(failure.Status(), failure.ToJSON())
	context.Abort()
}

func newFailure(exception exceptions.IException, status int) failure {
	return failure{
		exception: exception,
		status:    status,
	}
}

type failure struct {
	exception exceptions.IException
	status    int
}

func (failure failure) Status() int {
	return failure.status
}

func (failure failure) ToJSON() responses.Error {
	if exception, ok := failure.exception.(exceptions.AggregateException); ok {
		if errors, ok := exception.ToJSON().([]interface{}); ok {
			return responses.Error{
				Errors: errors,
			}
		}
	}

	return responses.Error{
		Errors: []interface{}{failure.exception.ToJSON()},
	}
}
