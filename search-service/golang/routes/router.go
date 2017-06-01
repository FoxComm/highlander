package routes

import (
    "github.com/FoxComm/highlander/search-service/golang/controllers"
    "github.com/gin-gonic/gin"
)

type RouterConfiguration struct {
    Engine *gin.Engine
    Routes map[string]controllers.IController
}

func SetUp(configuration RouterConfiguration) *gin.Engine {
    for route, controller := range configuration.Routes {
        controller.SetUp(configuration.Engine.Group(route))
    }

    return configuration.Engine
}
