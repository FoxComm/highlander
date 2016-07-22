package controllers

import (
	"github.com/gin-gonic/gin"
)

type pingController struct {
}

func NewPingController() IController {
	return &pingController{}
}

func (controller *pingController) SetUp(router gin.IRouter) {
	router.GET("/", controller.HealthCheck())
}

func (controller *pingController) HealthCheck() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.JSON(200, map[string]string{})
	}
}
