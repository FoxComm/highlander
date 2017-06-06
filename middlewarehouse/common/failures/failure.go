package failures

import (
	"github.com/gin-gonic/gin"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/errors"
)

type Failure interface {
	Status() int
	ToJSON() responses.ErrorResponse
}

func Abort(context *gin.Context, failure Failure) {
	context.JSON(failure.Status(), failure.ToJSON())
	context.Abort()
}

func toJSON(err error) responses.ErrorResponse {
	if err, ok := err.(*errors.AggregateError); ok {
		response, resErr := responses.NewReservationError(err.Errors)
		if resErr != nil {
			return responses.Error{
				Errors: err.Messages(),
			}
		} else {
			return *response
		}
	}

	return responses.Error{
		Errors: []string{err.Error()},
	}
}
