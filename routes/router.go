package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/gin-gonic/gin"
)

type RouterConfiguration struct {
	Engine *gin.Engine
	Routes map[string]controllers.IController
}

func SetUp(configuration RouterConfiguration) *gin.Engine {
	// prefix all routes with "/v1"
	r := configuration.Engine.Group("/v1")

	for route, controller := range configuration.Routes {
		controller.SetUp(r.Group(route))
	}

	return configuration.Engine
}
