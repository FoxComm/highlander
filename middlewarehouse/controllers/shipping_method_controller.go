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
	router.GET("/", controller.GetShippingMethods())
	router.GET("/:id", controller.GetShippingMethodByID())
	router.POST("/", controller.CreateShippingMethod())
	router.PUT("/:id", controller.UpdateShippingMethod())
	router.DELETE("/:id", controller.DeleteShippingMethod())
}

func (controller *shippingMethodController) GetShippingMethods() gin.HandlerFunc {
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

func (controller *shippingMethodController) GetShippingMethodByID() gin.HandlerFunc {
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

func (controller *shippingMethodController) CreateShippingMethod() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.ShippingMethod{}
		if parse(context, payload) != nil {
			return
		}

		//try create
		shippingMethod, err := controller.service.CreateShippingMethod(models.NewShippingMethodFromPayload(payload))
		if err == nil {
			context.JSON(http.StatusCreated, responses.NewShippingMethodFromModel(shippingMethod))
		} else {
			handleServiceError(context, err)
		}
	}
}

func (controller *shippingMethodController) UpdateShippingMethod() gin.HandlerFunc {
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
		model := models.NewShippingMethodFromPayload(payload)
		model.ID = id
		shippingMethod, err := controller.service.UpdateShippingMethod(model)

		if err == nil {
			context.JSON(http.StatusOK, responses.NewShippingMethodFromModel(shippingMethod))
		} else {
			handleServiceError(context, err)
		}
	}
}

func (controller *shippingMethodController) DeleteShippingMethod() gin.HandlerFunc {
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
