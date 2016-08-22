package main

import (
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/routes"

	"log"
	"github.com/gin-gonic/gin"
)

func engine() (*gin.Engine, error) {
	db, err := config.DefaultConnection()
	if err != nil {
		return nil, err
	}

	configuration := routes.RouterConfiguration{
		Engine: gin.Default(),
		Routes: routes.GetRoutes(db),
	}

	return routes.SetUp(configuration), nil
}

func main() {
	engine, err := engine()
	if err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	engine.Run(":9292")
}
