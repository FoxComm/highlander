package main

import (
	"github.com/FoxComm/middlewarehouse/routes"
	"github.com/gin-gonic/gin"
)

func main() {
	routes.Run(routes.RouterConfiguration{
		Engine:   gin.Default(),
		Endpoint: ":9292",
		Routes:   routes.Routes,
	})
}
