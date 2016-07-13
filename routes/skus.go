package routes

import (
	"github.com/gin-gonic/gin"
	"github.com/FoxComm/middlewarehouse/api/responses"
)

func runSkus(router gin.IRouter) {
	router.GET("/:code/summary", func(c *gin.Context) {
		summary := responses.NewSKUSummary()
		c.JSON(200, summary)
	})
}
