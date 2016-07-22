package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type carrierController struct {
	service services.ICarrierService
}

func NewCarrierController(service services.ICarrierService) IController {
	return &carrierController{service}
}

func (controller *carrierController) SetUp(router gin.IRouter) {
	router.GET("/", controller.GetCarriers())
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
