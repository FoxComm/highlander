package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/gin-gonic/gin"
)

type RouterConfiguration struct {
	Engine      *gin.Engine
	Endpoint    string
	Controllers []controllers.IController
}

func Run(configuration RouterConfiguration) {
	for _, controller := range configuration.Controllers {
		controller.SetUp()
	}

	configuration.Engine.Run(configuration.Endpoint)
}
