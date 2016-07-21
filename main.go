package main

import (
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/routes"
	"github.com/gin-gonic/gin"
)

func main() {
	engine := gin.Default()
	db, _ := config.DefaultConnection()
	routes.Run(routes.RouterConfiguration{
		Engine:      engine,
		Endpoint:    ":9292",
		Controllers: routes.GetControllers(engine, db),
	})
}
