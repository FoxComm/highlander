package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type reservationController struct {
	service services.IInventoryService
}

func NewReservationController(service services.IInventoryService) IController {
	return &reservationController{service}
}

func (controller *reservationController) SetUp(router gin.IRouter) {
	router.POST("/reserve", controller.Reserve())
	router.POST("/cancel", controller.Cancel())
}

func (controller *reservationController) Reserve() gin.HandlerFunc {
	return func(context *gin.Context) {
		var payload payloads.Reservation
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			context.AbortWithError(http.StatusBadRequest, err)
			return
		}

		skuMap := map[string]int{}
		for _, sku := range payload.SKUs {
			skuMap[sku.SKU] = int(sku.Qty)
		}

		if err := controller.service.ReserveItems(payload.RefNum, skuMap); err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, gin.H{})
	}
}

func (controller *reservationController) Cancel() gin.HandlerFunc {
	return func(context *gin.Context) {
		var payload payloads.Release
		if parse(context, &payload) != nil {
			return
		}

		if err := controller.service.ReleaseItems(payload.RefNum); err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, gin.H{})
	}
}
