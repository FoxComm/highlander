package controllers

import (
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type summaryController struct {
	service services.ISummaryService
}

func NewSummaryController(service services.ISummaryService) IController {
	return &summaryController{service}
}

func (controller *summaryController) SetUp(router gin.IRouter) {
	router.GET("/", controller.GetSummary())
	router.GET("/:code", controller.GetSummaryBySKU())
}

func (controller *summaryController) GetSummary() gin.HandlerFunc {
	return func(context *gin.Context) {
		summary, err := controller.service.GetSummary()
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp := responses.NewSummaryFromModel(summary)

		context.JSON(200, resp)
	}
}

func (controller *summaryController) GetSummaryBySKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		skuCode := context.Params.ByName("code")
		summary, err := controller.service.GetSummaryBySKU(skuCode)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp := responses.NewSummaryFromModel(summary)

		context.JSON(200, resp)
	}
}
