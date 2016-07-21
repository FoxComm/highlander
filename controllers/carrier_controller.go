package controllers

import (
	"net/http"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
)

type CarrierController struct {
	router  gin.IRouter
	service services.ICarrierService
}

func NewCarrierController(router gin.IRouter, service services.ICarrierService) IController {
	return &CarrierController{router, service}
}

func (controller *CarrierController) SetUp() {
	controller.router.GET("/", controller.GetCarriers())
}

func (controller *CarrierController) GetCarriers() gin.HandlerFunc {
	return func(context *gin.Context) {
		carriers, err := controller.service.Get()
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
