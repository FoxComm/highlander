package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type reservationController struct {
	service services.IInventoryService
}

func NewReservationController(service services.IInventoryService) IController {
	return &reservationController{service}
}

func (controller *reservationController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.POST("hold", controller.Hold())
	router.DELETE("hold/:refNum", controller.Unhold())
}

func (controller *reservationController) Hold() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Reservation{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		if err := payload.Validate(); err != nil {
			handleServiceError(context, err)
			return
		}

		if err := controller.service.HoldItems(payload); err != nil {
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
