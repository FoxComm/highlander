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
		shippingMethods, err := controller.service.GetShippingMethods()
		//ensure fetched successfully
		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		//convert to responses slice
		response := make([]*responses.ShippingMethod, len(shippingMethods))
		for i := range shippingMethods {
			response[i] = responses.NewShippingMethodFromModel(shippingMethods[i])
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
		shippingMethod, err := controller.service.GetShippingMethodByID(id)
		if err == nil {
			context.JSON(http.StatusOK, responses.NewShippingMethodFromModel(shippingMethod))
		} else {
			handleServiceError(context, err)
		}
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
		model, err := models.NewShippingMethodFromPayload(payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		shippingMethod, err := controller.service.CreateShippingMethod(model)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusCreated, responses.NewShippingMethodFromModel(shippingMethod))
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
		model, err := models.NewShippingMethodFromPayload(payload)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		model.ID = id
		shippingMethod, err := controller.service.UpdateShippingMethod(model)

		if err == nil {
			context.JSON(http.StatusOK, responses.NewShippingMethodFromModel(shippingMethod))
		} else {
			handleServiceError(context, err)
		}
	}
}

func (controller *shippingMethodController) deleteShippingMethod() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if err := controller.service.DeleteShippingMethod(id); err == nil {
			context.Status(http.StatusNoContent)
		} else {
			handleServiceError(context, err)
		}
	}
}
