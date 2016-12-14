package main

import (
	"log"
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	dbConfig "github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/middlewares"
	"github.com/FoxComm/highlander/middlewarehouse/routes"
	"github.com/FoxComm/highlander/middlewarehouse/tracer"

	"github.com/gin-gonic/gin"
	"github.com/opentracing/opentracing-go"
)

func setRoutes(engine *gin.Engine) error {
	db, err := dbConfig.DefaultConnection()
	if err != nil {
		return err
	}

	configuration := routes.RouterConfiguration{
		Engine: engine,
		Routes: routes.GetRoutes(db),
	}

	routes.SetUp(configuration)

	return nil
}

func setTracer(engine *gin.Engine) error {
	tr, err := tracer.NewTracer()
	if err != nil {
		return err
	}

	// explicitely set our tracer to be the default tracer.
	opentracing.InitGlobalTracer(tr)

	engine.Use(middlewares.TraceFromHTTPRequest(tr))

	return nil
}

func main() {
	if err := config.InitializeSiteConfig(); err != nil {
		log.Panicf("Failed to initialize middlewarehouse config with error %s", err.Error())
	}

	engine := gin.Default()

	if err := setTracer(engine); err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	if err := setRoutes(engine); err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	port := os.Getenv("PORT")
	engine.Run(":" + port)
}
