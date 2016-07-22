package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

type carrierController struct {
	service services.ICarrierService
}

func NewCarrierController(service services.ICarrierService) IController {
	return &carrierController{service}
}

func (controller *carrierController) SetUp(router gin.IRouter) {
	router.GET("/", controller.GetCarriers())
	router.GET("/:id", controller.GetCarrierByID())
	router.POST("/", controller.CreateCarrier())
	router.PUT("/:id", controller.UpdateCarrier())
	router.DELETE("/:id", controller.DeleteCarrier())
}

func (controller *carrierController) GetCarriers() gin.HandlerFunc {
	return func(context *gin.Context) {
		carriers, err := controller.service.GetCarriers()
		//ensure fetched successfully
		if err != nil {
			context.AbortWithError(http.StatusInternalServerError, err)
			return
		}

		//convert to responses slice
		response := make([]*responses.Carrier, len(carriers))
		for i := range carriers {
			response[i] = responses.NewCarrierFromModel(carriers[i])
		}
		context.JSON(http.StatusOK, response)
	}
}

func (controller *carrierController) GetCarrierByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get carrier by id
		carrier, err := controller.service.GetCarrierByID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) CreateCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.Carrier{}
		if parse(context, payload) != nil {
			return
		}

		//try create
		model := models.NewCarrierFromPayload(payload)
		if id, err := controller.service.CreateCarrier(model); err == nil {
			context.JSON(http.StatusCreated, id)
		} else {
			context.AbortWithError(http.StatusBadRequest, err)
		}
	}
}

func (controller *carrierController) UpdateCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.Carrier{}
		if parse(context, payload) != nil {
			return
		}

		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//try update
		model := models.NewCarrierFromPayload(payload)
		model.ID = id
		if err := controller.service.UpdateCarrier(model); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Writer.WriteHeader(http.StatusNoContent)
	}
}

func (controller *carrierController) DeleteCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if err := controller.service.DeleteCarrier(id); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Writer.WriteHeader(http.StatusNoContent)
	}
}
