package routes

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func runStockItems(router *gin.Engine) {
	router.GET("/stock-items/:id", func(c *gin.Context) {
		//id, fail := paramInt(c, "id")

		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
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
		var json payloads.StockItem
		if parse(c, &json) != nil {
			return
		}

		invMgr, err := initInventoryManager(c)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		resp, err := invMgr.CreateStockItem(&json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.JSON(http.StatusCreated, resp)
	})

	router.PATCH("/stock-items/:id/units", func(c *gin.Context) {
		var json payloads.StockItemUnits
		if parse(c, &json) != nil {
			return
		}

		invMgr, err := initInventoryManager(c)
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		if json.Qty > 0 {
			err := invMgr.IncrementStockItemUnits(&json)
			if err != nil {
				c.AbortWithError(http.StatusBadRequest, err)
				return
			}

			c.JSON(http.StatusCreated, gin.H{})
		} else {
			err := errors.New("Not implemented")
			c.AbortWithError(http.StatusBadRequest, err)
		}
	})
}
