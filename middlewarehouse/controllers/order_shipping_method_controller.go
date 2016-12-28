package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
)

type orderShippingMethodController struct {
	service services.IShippingMethodService
}

func NewOrderShippingMethodController(service services.IShippingMethodService) IController {
	return &orderShippingMethodController{service}
}

func (controller *orderShippingMethodController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.POST("", controller.getOrderShippingMethods())
}

func (controller *orderShippingMethodController) getOrderShippingMethods() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.Order{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		resp, err := controller.service.EvaluateForOrder(payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, resp)
	}
}
