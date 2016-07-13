package routes

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"
)

func runReservations(router gin.IRouter) {
	router.GET("/:skuCode", func(c *gin.Context) {
		c.JSON(http.StatusNotImplemented, gin.H{})
	})

	router.PATCH("/reserve", func(c *gin.Context) {
		mgr, err := services.MakeInventoryManager()

		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		var json payloads.Reservation
		if parse(c, &json) != nil {
			return
		}

		if err := mgr.ReserveItems(json); err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
		}

		c.JSON(http.StatusCreated, gin.H{})
	})

	router.PATCH("/unreserve", func(c *gin.Context) {
		c.JSON(http.StatusNotImplemented, gin.H{})
	})
}
