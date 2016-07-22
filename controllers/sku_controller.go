package controllers

import (
	"github.com/FoxComm/middlewarehouse/api/responses"

	"github.com/gin-gonic/gin"
)

type skuController struct {
}

func NewSKUController() IController {
	return &skuController{}
}

func (controller *skuController) SetUp(router gin.IRouter) {
	router.GET("/:code/summary", controller.GetSKUs())
}

func (controller *skuController) GetSKUs() gin.HandlerFunc {
	return func(context *gin.Context) {
		summary := responses.NewSKUSummary()
		context.JSON(200, summary)
	}
}
