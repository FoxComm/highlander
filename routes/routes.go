package routes

import (
	"github.com/gin-gonic/gin"
)

func Run(engine *gin.Engine, endpoint string) {
	for route, handler := range getRoutes() {
		handler(engine.Group(route))
	}

	engine.Run(endpoint)
}

func getRoutes() map[string]func(gin.IRouter) {
	return map[string]func(gin.IRouter){
		"/carriers":     runCarriers,
		"/reservations": runReservations,
		"/skus":         runSkus,
		"/stock-items":  runStockItems,
	}
}
