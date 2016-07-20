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

// /stock-items router group handler
func runStockItems(router gin.IRouter) {
	router.GET("/:id", func(c *gin.Context) {
		idStr := c.Params.ByName("id")

		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		mgr, err := services.MakeInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		resp, err := mgr.FindStockItemByID(uint(id))
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

	router.POST("/", func(c *gin.Context) {
		var json payloads.StockItem
		if parse(c, &json) != nil {
			return
		}

		mgr, err := services.MakeInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		resp, err := mgr.CreateStockItem(&json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.JSON(http.StatusCreated, resp)
	})

	router.PATCH("/:id/increment", func(c *gin.Context) {
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

		mgr, err := services.MakeInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		err = mgr.IncrementStockItemUnits(uint(id), &json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.JSON(http.StatusCreated, gin.H{})
	})

	router.PATCH("/:id/decrement", func(c *gin.Context) {
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

		mgr, err := services.MakeInventoryManager()
		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		err = mgr.DecrementStockItemUnits(uint(id), &json)
		if err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		c.Status(http.StatusNoContent)
	})
}
