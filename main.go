package main

import (
	"github.com/FoxComm/middlewarehouse/routes"
	"github.com/gin-gonic/gin"
)

func main() {
	routes.Run(gin.Default(), ":9292")
}
