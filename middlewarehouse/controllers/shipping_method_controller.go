package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type shippingMethodController struct {
	service services.IShippingMethodService
}

func NewShippingMethodController(service services.IShippingMethodService) IController {
	return &shippingMethodController{service}
}

func (controller *shippingMethodController) SetUp(router gin.IRouter) {
	router.GET("", controller.getShippingMethods())
	router.GET(":id", controller.getShippingMethodByID())
	router.POST("", controller.createShippingMethod())
	router.PUT(":id", controller.updateShippingMethod())
	router.DELETE(":id", controller.deleteShippingMethod())
}

func (controller *shippingMethodController) getShippingMethods() gin.HandlerFunc {
	return func(context *gin.Context) {
		shippingMethods, exception := controller.service.GetShippingMethods()
		//ensure fetched successfully
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		//convert to responses slice
		response := make([]*responses.ShippingMethod, len(shippingMethods))
		for i := range shippingMethods {
			resp, exception := responses.NewShippingMethodFromModel(shippingMethods[i])
			if exception != nil {
				handleServiceError(context, exception)
				return
			}

			response[i] = resp
		}
		context.JSON(http.StatusOK, response)
	}
}

func (controller *shippingMethodController) getShippingMethodByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get shippingMethod by id
		shippingMethod, exception := controller.service.GetShippingMethodByID(id)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		resp, exception := responses.NewShippingMethodFromModel(shippingMethod)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusOK, resp)
	}
}

func (controller *shippingMethodController) createShippingMethod() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.ShippingMethod{}
		if parse(context, payload) != nil {
			return
		}

		//try create
		model, exception := models.NewShippingMethodFromPayload(payload)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		shippingMethod, exception := controller.service.CreateShippingMethod(model)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		resp, exception := responses.NewShippingMethodFromModel(shippingMethod)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusCreated, resp)
	}
}

func (controller *shippingMethodController) updateShippingMethod() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.ShippingMethod{}
		if parse(context, payload) != nil {
			return
		}

		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//try update
		model, exception := models.NewShippingMethodFromPayload(payload)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		model.ID = id
		shippingMethod, exception := controller.service.UpdateShippingMethod(model)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		resp, exception := responses.NewShippingMethodFromModel(shippingMethod)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusOK, resp)
	}
}

func (controller *shippingMethodController) deleteShippingMethod() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if exception := controller.service.DeleteShippingMethod(id); exception == nil {
			context.Status(http.StatusNoContent)
		} else {
			handleServiceError(context, exception)
		}
	}
}
