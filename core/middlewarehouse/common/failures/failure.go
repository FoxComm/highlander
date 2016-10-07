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

func Abort(c *gin.Context, f Failure) {
	c.JSON(f.Status(), f.ToJSON())
	c.Abort()
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
