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

		if exception := payload.Validate(); exception != nil {
			fail := failures.NewBadRequest(exception)
			failures.Abort(context, fail)
			return
		}

		skuMap := map[string]int{}
		for _, sku := range payload.Items {
			skuMap[sku.SKU] = int(sku.Qty)
		}

		if exception := controller.service.HoldItems(payload.RefNum, skuMap); exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.Status(http.StatusNoContent)
	}
}

func (controller *reservationController) Unhold() gin.HandlerFunc {
	return func(context *gin.Context) {
		refNum := context.Params.ByName("refNum")

		if exception := controller.service.ReleaseItems(refNum); exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
