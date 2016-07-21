package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/FoxComm/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

var Routes = map[string]func(gin.IRouter){
	"/carriers":     runCarriers,
	"/reservations": runReservations,
	"/skus":         runSkus,
	"/stock-items":  runStockItems,
}

func GetRoutes(db *gorm.DB) map[string]controllers.IController {
	return map[string]controllers.IController{
		"/carriers": controllers.NewCarrierController(services.NewCarrierService(db)),
	}
}
