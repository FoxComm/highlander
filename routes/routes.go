package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/jinzhu/gorm"
)

func GetRoutes(db *gorm.DB) map[string]controllers.IController {
	summaryService := services.NewSummaryService(db)
	inventoryService := services.NewInventoryService(db, summaryService)
	carrierService := services.NewCarrierService(db)

	return map[string]controllers.IController{
		"/skus": controllers.NewSKUController(),
		"/stock-items": controllers.NewStockItemController(inventoryService),
		"/reservations": controllers.NewReservationController(inventoryService),
		"/carriers": controllers.NewCarrierController(carrierService),
	}
}
