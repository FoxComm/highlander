package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"

	"github.com/gin-gonic/gin"
)

type reservationController struct {
	service services.IInventoryService
}

func NewReservationController(service services.IInventoryService) IController {
	return &reservationController{service}
}

func (controller *reservationController) SetUp(router gin.IRouter) {
	router.POST("hold", controller.Hold())
	router.DELETE("hold/:refNum", controller.Unhold())
}

func (controller *reservationController) Hold() gin.HandlerFunc {
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
		for _, sku := range payload.Items {
			skuMap[sku.SKU] = int(sku.Qty)
		}

		if err := controller.service.HoldItems(payload.RefNum, skuMap); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}

func (controller *reservationController) Unhold() gin.HandlerFunc {
	return func(context *gin.Context) {
		refNum := context.Params.ByName("refNum")

		if err := controller.service.ReleaseItems(refNum); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
