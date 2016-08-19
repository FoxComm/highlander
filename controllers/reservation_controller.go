package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/FoxComm/middlewarehouse/common/failures"
	"github.com/gin-gonic/gin"
)

type reservationController struct {
	service services.IInventoryService
}

func NewReservationController(service services.IInventoryService) IController {
	return &reservationController{service}
}

func (controller *reservationController) SetUp(router gin.IRouter) {
	router.POST("reserve", controller.Reserve())
	router.POST("cancel", controller.Cancel())
}

func (controller *reservationController) Reserve() gin.HandlerFunc {
	return func(context *gin.Context) {
		var payload payloads.Reservation
		if parse(context, &payload) != nil {
			return
		}

		if err := payload.Validate(); err != nil {
			fail := failures.NewBadRequest(err)
			failures.Abort(context, fail)
			return
		}

		skuMap := map[string]int{}
		for _, sku := range payload.SKUs {
			skuMap[sku.SKU] = int(sku.Qty)
		}

		if err := controller.service.HoldItems(payload.RefNum, skuMap); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
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

		context.Status(http.StatusNoContent)
	}
}
