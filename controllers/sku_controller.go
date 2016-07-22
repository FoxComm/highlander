package controllers

import (
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/gin-gonic/gin"
)

type skuController struct {
	summaryService services.ISummaryService
}

func NewSKUController(service services.ISummaryService) IController {
	return &skuController{summaryService: service}
}

func (controller *skuController) SetUp(router gin.IRouter) {
	router.GET("/summary", controller.GetSummaries())
	router.GET("/summary/:code", controller.GetSummaryBySKU())
}

func (controller *skuController) GetSummaries() gin.HandlerFunc {
	return func(context *gin.Context) {
		summary, err := controller.summaryService.GetSummaries()
		if err != nil {
			handleServiceError(context, err)
			return
		}

		context.JSON(200, summary)
	}
}

func (controller *skuController) GetSummaryBySKU() gin.HandlerFunc {
	return func(context *gin.Context) {
		skuCode := context.Params.ByName("code")
		summary, err := controller.summaryService.GetSummaryBySKU(skuCode)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp := responses.NewSKUSummaryFromModel(skuCode, summary)

		context.JSON(200, resp)
	}
}
