package controllers

import (
	"net/http"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/FoxComm/highlander/middlewarehouse/common/failures"
	"github.com/gin-gonic/gin"
)

type stockLocationController struct {
	service services.StockLocationService
}

func NewStockLocationController(service services.StockLocationService) IController {
	return &stockLocationController{service}
}

func (controller *stockLocationController) SetUp(router gin.IRouter) {
	router.Use(FetchJWT)
	router.GET("", controller.GetLocations())
	router.GET(":id", controller.GetLocationByID())
	router.POST("", controller.CreateLocation())
	router.PUT(":id", controller.UpdateLocation())
	router.DELETE(":id", controller.DeleteLocation())
}

func (controller *stockLocationController) GetLocations() gin.HandlerFunc {
	return func(context *gin.Context) {
		locations, err := controller.service.GetLocations()
		if err != nil {
			fail := failures.NewInternalError(err)
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

		location, err := controller.service.GetLocationByID(id)
		if err != nil {
			handleServiceError(context, err)
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

		if !setScope(context, payload) {
			return
		}

		model := payload.Model()
		location, err := controller.service.CreateLocation(model)
		if err != nil {
			handleServiceError(context, err)
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

		model := payload.Model()
		model.ID = id

		location, err := controller.service.UpdateLocation(model)
		if err != nil {
			handleServiceError(context, err)
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

		if err := controller.service.DeleteLocation(id); err != nil {
			handleServiceError(context, err)
			return
		}

		context.Status(http.StatusNoContent)
	}
}
