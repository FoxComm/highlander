package routes

import (
	"github.com/FoxComm/highlander/middlewarehouse/controllers"
<<<<<<< Updated upstream
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
=======
>>>>>>> Stashed changes
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/jinzhu/gorm"
)

func GetRoutes(db *gorm.DB) map[string]controllers.IController {
	//repositories
	carrierRepository := repositories.NewCarrierRepository(db)
	shippingMethodRepository := repositories.NewShippingMethodRepository(db)

	//services
	summaryService := services.NewSummaryService(db)
	inventoryService := services.NewInventoryService(db, summaryService)
	carrierService := services.NewCarrierService(carrierRepository)
	shippingMethodService := services.NewShippingMethodService(shippingMethodRepository)

	return map[string]controllers.IController{
		"/ping":             controllers.NewPingController(),
		"/summary":          controllers.NewSummaryController(summaryService),
		"/stock-items":      controllers.NewStockItemController(inventoryService),
		"/reservations":     controllers.NewReservationController(inventoryService),
		"/carriers":         controllers.NewCarrierController(carrierService),
		"/shipping-methods": controllers.NewShippingMethodController(shippingMethodService),
	}
}
