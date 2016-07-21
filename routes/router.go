package routes

import (
	"github.com/gin-gonic/gin"
)

type RouterConfiguration struct {
	Engine   *gin.Engine
	Endpoint string
	Routes   map[string]func(gin.IRouter)
}

func Run(configuration RouterConfiguration) {
	for route, handler := range configuration.Routes {
		handler(configuration.Engine.Group(route))
	}

	configuration.Engine.Run(configuration.Endpoint)
}
