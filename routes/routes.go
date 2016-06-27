package routes

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/gin-gonic/gin"
)

func Run() {
	router := gin.Default()
	router.GET("/skus/:code/summary", func(c *gin.Context) {
		summary := responses.NewSKUSummary()
		c.JSON(200, summary)
	})

	fmt.Println("Starting middlewarehouse...")
	router.Run(":9292")
	fmt.Println("middlewarehouse started on port 9292")
}
