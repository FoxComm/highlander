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

func toJSON(err error) responses.Error {
	if err, ok := err.(errors.AggregateError); ok {
		return responses.Error{
			Errors: err.Messages(),
		}
	}

	return responses.Error{
		Errors: []string{err.Error()},
	}
}
