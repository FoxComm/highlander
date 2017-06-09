package main

import (
    "os"
    "fmt"

    "github.com/gin-gonic/gin"

    "github.com/FoxComm/highlander/search-service/golang/routes"
)

func main() {
    engine, err := engine()
    if err != nil {
        panic(fmt.Errorf("Failed to start search service with error %s", err.Error()))
    }

    port := os.Getenv("PORT")
    engine.Run(":" + port)
}

func engine() (*gin.Engine, error) {
    r := gin.New()

    r.Use(gin.Recovery())

    configuration := routes.RouterConfiguration{
        Engine: r,
        Routes: routes.GetRoutes(),
    }

    return routes.SetUp(configuration), nil
}
