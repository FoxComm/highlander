package main

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	dbConfig "github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/middlewares"
	"github.com/FoxComm/highlander/middlewarehouse/routes"
	"github.com/FoxComm/highlander/middlewarehouse/tracer"

	"github.com/gin-gonic/gin"
	"github.com/opentracing/opentracing-go"
)

func setRoutes(appConfig *config.AppConfig, engine *gin.Engine) error {
	db, err := dbConfig.DefaultConnection()
	if err != nil {
		return err
	}

	configuration := routes.RouterConfiguration{
		Engine: engine,
		Routes: routes.GetRoutes(appConfig, db),
	}

	routes.SetUp(configuration)

	return nil
}

func setTracer(appConfig *config.AppConfig, engine *gin.Engine) error {
	tracerConfig, err := config.NewTracerConfig()
	if err != nil {
		log.Panicf("Failed to initialize middlewarehouse tracer config with error %s", err.Error())
	}

	tr, err := tracer.NewTracer(tracerConfig, appConfig.Port)
	if err != nil {
		return err
	}

	// explicitely set our tracer to be the default tracer.
	opentracing.InitGlobalTracer(tr)

	engine.Use(middlewares.TraceFromHTTPRequest(tr))

	return nil
}

func main() {
	appConfig, err := config.NewAppConfig()
	if err != nil {
		log.Panicf("Failed to initialize middlewarehouse config with error %s", err.Error())
	}

	engine := gin.Default()

	if err := setTracer(appConfig, engine); err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	if err := setRoutes(appConfig, engine); err != nil {
		log.Panicf("Failed to start middlewarehouse with error %s", err.Error())
	}

	engine.Run(":" + appConfig.Port)
}
