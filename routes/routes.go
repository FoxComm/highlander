package routes

import (
	"fmt"

	"github.com/gin-gonic/gin"
)

func Run() {
	router := gin.Default()

	runSkus(router.Group("/skus"))
	runStockItems(router.Group("/stock-items"))
	runReservations(router.Group("/reservations"))

	fmt.Println("Starting middlewarehouse...")
	router.Run(":9292")
	fmt.Println("middlewarehouse started on port 9292")
}
