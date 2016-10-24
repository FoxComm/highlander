package controllers

import (
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
)

type summaryController struct {
	service services.ISummaryService
}

func NewSummaryController(service services.ISummaryService) IController {
	return &summaryController{service}
}

func (controller *summaryController) SetUp(router gin.IRouter) {
	router.GET("", controller.GetSummary())
	router.GET("skuId/:id", controller.GetSummaryBySkuID())
	router.GET("skuCode/:code", controller.GetSummaryBySkuCode())
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

func (controller *summaryController) GetSummaryBySkuCode() gin.HandlerFunc {
	return func(context *gin.Context) {
		skuCode := context.Params.ByName("code")
		summary, err := controller.service.GetSummaryBySkuCode(skuCode)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp := responses.NewSummaryFromModel(summary)

		context.JSON(200, resp)
	}
}

func (controller *summaryController) GetSummaryBySkuID() gin.HandlerFunc {
	return func(context *gin.Context) {
		id, fail := paramUint(context, "id")
		if fail != nil {
			return
		}

		summary, err := controller.service.GetSummaryBySkuID(id)
		if err != nil {
			handleServiceError(context, err)
			return
		}

		resp := responses.NewSummaryFromModel(summary)

		context.JSON(200, resp)
	}
}
