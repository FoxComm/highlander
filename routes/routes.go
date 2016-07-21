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

func GetControllers(engine *gin.Engine, db *gorm.DB) []controllers.IController {
	return []controllers.IController{
		controllers.NewCarrierController(engine.Group("/carriers"), services.NewCarrierService(db)),
	}
}
