package routes

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"
)

func runReservations(router gin.IRouter) {
	router.POST("/hold", func(c *gin.Context) {
		var json payloads.Reservation
		if parse(c, &json) != nil {
			return
		}

		if err := json.Validate(); err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
			return
		}

		mgr, err := services.MakeInventoryManager()

		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		if err := mgr.ReserveItems(json); err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
		}

		c.JSON(http.StatusOK, gin.H{})
	})

	router.POST("/unhold", func(c *gin.Context) {
		var json payloads.Release
		if parse(c, &json) != nil {
			return
		}

		mgr, err := services.MakeInventoryManager()

		if err != nil {
			c.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		if err := mgr.ReleaseItems(json); err != nil {
			c.AbortWithError(http.StatusBadRequest, err)
		}

		c.JSON(http.StatusOK, gin.H{})
	})
}
