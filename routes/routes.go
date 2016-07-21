package routes

import (
	"github.com/gin-gonic/gin"
)

var Routes = map[string]func(gin.IRouter){
	"/carriers":     runCarriers,
	"/reservations": runReservations,
	"/skus":         runSkus,
	"/stock-items":  runStockItems,
}
