package routes

import (
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/FoxComm/middlewarehouse/repositories"
	"github.com/FoxComm/middlewarehouse/services"

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
	// shipmentService := services.NewShipmentService(db)

	return map[string]controllers.IController{
		"/ping":            controllers.NewPingController(),
		"/summary":         controllers.NewSummaryController(summaryService),
		"/stock-items":     controllers.NewStockItemController(inventoryService),
		"/reservations":    controllers.NewReservationController(inventoryService),
		"/carriers":        controllers.NewCarrierController(carrierService),
		"/shippingMethods": controllers.NewShippingMethodController(shippingMethodService),
		// "/shipments":    controllers.NewShipments(shipmentService),
	}
}
