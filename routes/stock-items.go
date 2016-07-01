package routes

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func runStockItems(router *gin.Engine) {
	router.GET("/stock-items/:id", func(c *gin.Context) {
		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		invMgr, err := initInventoryManager(c)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		resp, err := invMgr.FindStockItemByID(uint(id))
		if err != nil {
			if err == gorm.ErrRecordNotFound {
				c.AbortWithStatus(http.StatusNotFound)
			} else {
				c.AbortWithError(http.StatusInternalServerError, err)
			}
			return
		}

		c.JSON(http.StatusOK, resp)
	})

	router.POST("/stock-items", func(c *gin.Context) {
		c.JSON(201, gin.H{})
	})

	router.PATCH("/stock-items/:id/units", func(c *gin.Context) {
		c.JSON(201, gin.H{})
	})
}
