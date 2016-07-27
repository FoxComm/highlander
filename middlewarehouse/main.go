package main

import (
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/routes"

	"github.com/gin-gonic/gin"
)

func getEngine() *gin.Engine {
	db, _ := config.DefaultConnection()

	configuration := routes.RouterConfiguration{
		Engine: gin.Default(),
		Routes: routes.GetRoutes(db),
	}

	return routes.SetUp(configuration)
}

func main() {
	getEngine().Run(":9292")
}
