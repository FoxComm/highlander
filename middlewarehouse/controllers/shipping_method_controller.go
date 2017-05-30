package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
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
	router.Use(FetchJWT)
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
			resp, err := responses.NewShippingMethodFromModel(shippingMethods[i])
			if err != nil {
				handleServiceError(context, err)
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
		shippingMethod, err := controller.service.GetShippingMethodByID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp, err := responses.NewShippingMethodFromModel(shippingMethod)
		if err != nil {
			handleServiceError(context, err)
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

		if !setScope(context, payload) {
			return
		}

		//try create
		model, err := payload.Model()
		if err != nil {
			handleServiceError(context, err)
			return
		}

		shippingMethod, err := controller.service.CreateShippingMethod(model)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp, err := responses.NewShippingMethodFromModel(shippingMethod)
		if err != nil {
			handleServiceError(context, err)
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
		model, err := payload.Model()
		if err != nil {
			handleServiceError(context, err)
			return
		}

		model.ID = id
		shippingMethod, err := controller.service.UpdateShippingMethod(model)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp, err := responses.NewShippingMethodFromModel(shippingMethod)
		if err != nil {
			handleServiceError(context, err)
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

		if err := controller.service.DeleteShippingMethod(id); err == nil {
			context.Status(http.StatusNoContent)
		} else {
			handleServiceError(context, err)
		}
	}
}
