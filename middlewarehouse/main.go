package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	dbConfig "github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/routes"
	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"

	"github.com/gin-gonic/gin"
)

func engine() (*gin.Engine, exceptions.IException) {
	db, exception := dbConfig.DefaultConnection()
	if exception != nil {
		return nil, exception
	}

	configuration := routes.RouterConfiguration{
		Engine: gin.Default(),
		Routes: routes.GetRoutes(db),
	}

	return routes.SetUp(configuration), nil
}

func main() {
	if exception := config.InitializeSiteConfig(); exception != nil {
		log.Panicf("Failed to initialize middlewarehouse config with error %s", exception.ToString())
	}

	engine, exception := engine()
	if exception != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", exception.ToString())
	}

	engine.Run(":9292")
}
