package main

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	dbConfig "github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/routes"

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
	if err := config.InitializeSiteConfig(); err != nil {
		log.Panicf("Failed to initialize middlewarehouse config with error %s", err.Error())
	}

	engine, err := engine()
	if err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	port := os.Getenv("PORT")
	engine.Run(":" + port)
}
