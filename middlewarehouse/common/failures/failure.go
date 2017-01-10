package failures

import (
	"github.com/gin-gonic/gin"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/errors"
)

// Failure is the FoxCommerce wrapper for errors. It givesus a reliable way to
// construct errors that can be shared with the user or logged as debug
// information.
type Failure interface {
	Error() string
	Status() int
	ToJSON() responses.Error
}

// NewFailure attempts to construct the most appropriate type of Failure based
// on the error that's passed in.
func NewFailure(err error) Failure {
	if err != nil {
		return &internalError{err: err}
	}

	return nil
}

func HandleFailuresHTTP(context *gin.Context, fail Failure) bool {
	if fail != nil {
		context.JSON(fail.Status(), fail.ToJSON())
		return true
	}

	return false
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
