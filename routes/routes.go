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
	summaryRepository := repositories.NewSummaryRepository(db)
	stockItemRepository := repositories.NewStockItemRepository(db)
	unitRepository := repositories.NewStockItemUnitRepository(db)
	stockLocationRepository := repositories.NewStockLocationRepository(db)
	shippingMethodRepository := repositories.NewShippingMethodRepository(db)
	shipmentRepository := repositories.NewShipmentRepository(db)
	shipmentLineItemRepository := repositories.NewShipmentLineItemRepository(db)

	//services
	summaryService := services.NewSummaryService(summaryRepository, stockItemRepository)
	inventoryService := services.NewInventoryService(stockItemRepository, unitRepository, summaryService)
	carrierService := services.NewCarrierService(carrierRepository)
	stockLocationService := services.NewStockLocationService(stockLocationRepository)
	shippingMethodService := services.NewShippingMethodService(shippingMethodRepository)
	shipmentLineItemService := services.NewShipmentLineItemService(shipmentLineItemRepository)
	shipmentService := services.NewShipmentService(db, shipmentRepository, shipmentLineItemService, unitRepository)

	return map[string]controllers.IController{
		"/public/ping":             controllers.NewPingController(),
		"/public/summary":          controllers.NewSummaryController(summaryService),
		"/public/stock-items":      controllers.NewStockItemController(inventoryService),
		"/public/stock-locations":  controllers.NewStockLocationController(stockLocationService),
		"/public/carriers":         controllers.NewCarrierController(carrierService),
		"/public/shipping-methods": controllers.NewShippingMethodController(shippingMethodService),
		"/public/shipments":        controllers.NewShipmentController(shipmentService, shipmentLineItemService),

		"/private/reservations": controllers.NewReservationController(inventoryService),
	}
}
