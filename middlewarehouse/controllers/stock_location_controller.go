package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/gin-gonic/gin"
)

type stockLocationController struct {
	service services.IStockLocationService
}

func NewStockLocationController(service services.IStockLocationService) IController {
	return &stockLocationController{service}
}

func (controller *stockLocationController) SetUp(router gin.IRouter) {
	router.GET("", controller.GetLocations())
	router.GET(":id", controller.GetLocationByID())
	router.POST("", controller.CreateLocation())
	router.PUT(":id", controller.UpdateLocation())
	router.DELETE(":id", controller.DeleteLocation())
}

func (controller *stockLocationController) GetLocations() gin.HandlerFunc {
	return func(context *gin.Context) {
		locations, exception := controller.service.GetLocations()
		if exception != nil {
			fail := failures.NewInternalError(exception)
			failures.Abort(context, fail)
			return
		}

		context.JSON(http.StatusOK, responses.NewStockLocationsFromModels(locations))
	}
}

func (controller *stockLocationController) GetLocationByID() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		location, exception := controller.service.GetLocationByID(id)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusOK, responses.NewStockLocationFromModel(location))
	}
}

func (controller *stockLocationController) CreateLocation() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.StockLocation{}
		if parse(context, payload) != nil {
			return
		}

		model := models.NewStockLocationFromPayload(payload)
		location, exception := controller.service.CreateLocation(model)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusCreated, responses.NewStockLocationFromModel(location))
	}
}

func (controller *stockLocationController) UpdateLocation() gin.HandlerFunc {
	return func(context *gin.Context) {
		payload := &payloads.StockLocation{}
		if parse(context, payload) != nil {
			return
		}

		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		model := models.NewStockLocationFromPayload(payload)
		model.ID = id

		location, exception := controller.service.UpdateLocation(model)
		if exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.JSON(http.StatusOK, responses.NewStockLocationFromModel(location))
	}
}

func (controller *stockLocationController) DeleteLocation() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, failure := paramUint(context, "id")
		if failure != nil {
			return
		}

		if exception := controller.service.DeleteLocation(id); exception != nil {
			handleServiceError(context, exception)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
