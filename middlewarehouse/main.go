package main

import (
	"log"

	dbConfig "github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/routes"

	"log"
	"github.com/gin-gonic/gin"
)

func engine() (*gin.Engine, error) {
	db, err := dbConfig.DefaultConnection()
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
	// TODO: Bring this back when moving capture to a consumer.
	// if err := config.InitializeSiteConfig(); err != nil {
	// 	log.Panicf("Failed to initialize middlewarehouse config with error %s", err.Error())
	// }

	engine, err := engine()
	if err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	engine.Run(":9292")
}
