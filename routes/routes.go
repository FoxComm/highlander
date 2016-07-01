package routes

import (
	"fmt"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/common/store"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
)

func initInventoryManager(c *gin.Context) (*services.InventoryMgr, error) {
	ctx, err := store.NewStoreContext(c)
	if err != nil {
		return nil, err
	}

	return services.NewInventoryMgr(ctx)
}

func Run() {
	router := gin.Default()
	router.GET("/skus/:code/summary", func(c *gin.Context) {
		summary := responses.NewSKUSummary()
		c.JSON(200, summary)
	})

	runStockItems(router)

	fmt.Println("Starting middlewarehouse...")
	router.Run(":9292")
	fmt.Println("middlewarehouse started on port 9292")
}
