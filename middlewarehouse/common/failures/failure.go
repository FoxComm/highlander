package failures

import (
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/gin-gonic/gin"
)

type Failure interface {
	Status() int
	ToJSON() responses.Error
}

func Abort(c *gin.Context, f Failure) {
	c.JSON(f.Status(), f.ToJSON())
	c.Abort()
}
