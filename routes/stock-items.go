package routes

import (
	"errors"
	"net/http"
	"strconv"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func runStockItems(router *gin.Engine) {
	router.GET("/stock-items/:id", func(c *gin.Context) {
		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		invMgr, err := services.NewInventoryManager()
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

		invMgr, err := services.NewInventoryManager()
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

	router.PATCH("/stock-items/:id/increment", func(c *gin.Context) {
		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		var json payloads.IncrementStockItemUnits
		if parse(c, &json) != nil {
			return
		}

		if json.Qty <= 0 {
			err := errors.New("Qty must be greater than 0")
			c.AbortWithError(http.StatusBadRequest, err)
		}

		invMgr, err := services.NewInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		err = invMgr.IncrementStockItemUnits(uint(id), &json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.JSON(http.StatusCreated, gin.H{})
	})

	router.PATCH("/stock-items/:id/decrement", func(c *gin.Context) {
		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		var json payloads.DecrementStockItemUnits
		if parse(c, &json) != nil {
			return
		}

		if json.Qty <= 0 {
			err := errors.New("Qty must be greater than 0")
			c.AbortWithError(http.StatusBadRequest, err)
		}

		invMgr, err := services.NewInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		err = invMgr.DecrementStockItemUnits(uint(id), &json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.Status(http.StatusNoContent)
	})
}
