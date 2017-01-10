package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type carrierController struct {
	service services.CarrierService
}

func NewCarrierController(service services.CarrierService) IController {
	return &carrierController{service}
}

func (controller *carrierController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.GET("", controller.getCarriers())
	router.GET(":id", controller.getCarrierByID())
	router.POST("", controller.createCarrier())
	router.PUT(":id", controller.updateCarrier())
	router.DELETE(":id", controller.deleteCarrier())
}

func (controller *carrierController) getCarriers() gin.HandlerFunc {
	return func(context *gin.Context) {
		carriers, fail := controller.service.GetCarriers()
		if failures.HandleFailuresHTTP(context, fail) {
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

func (controller *carrierController) getCarrierByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		//get id from context
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		//get carrier by id
		carrier, fail := controller.service.GetCarrierByID(id)
		if failures.HandleFailuresHTTP(context, fail) {
			return
		}

		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) createCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		//try parse payload
		payload := &payloads.Carrier{}
		if parse(context, payload) != nil {
			return
		}

		if !setScope(context, payload) {
			return
		}

		//try create
		carrier, fail := controller.service.CreateCarrier(payload.Model())
		if failures.HandleFailuresHTTP(context, fail) {
			return
		}

		context.JSON(http.StatusCreated, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) updateCarrier() gin.HandlerFunc {
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
		model := payload.Model()
		model.ID = id
		carrier, fail := controller.service.UpdateCarrier(model)

		if failures.HandleFailuresHTTP(context, fail) {
			return
		}

		context.JSON(http.StatusOK, responses.NewCarrierFromModel(carrier))
	}
}

func (controller *carrierController) deleteCarrier() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		fail := controller.service.DeleteCarrier(id)
		if failures.HandleFailuresHTTP(context, fail) {
			return
		}

		context.Status(http.StatusNoContent)
	}
}
