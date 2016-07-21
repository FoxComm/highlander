package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/gin-gonic/gin"
)

type RouterConfiguration struct {
	Engine   *gin.Engine
	Endpoint string
	Routes   map[string]controllers.IController
}

func Run(configuration RouterConfiguration) {
	for route, controller := range configuration.Routes {
		controller.SetUp(configuration.Engine.Group(route))
	}

	configuration.Engine.Run(configuration.Endpoint)
}
