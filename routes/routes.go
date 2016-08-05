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
	addressRepository := repositories.NewAddressRepository(db)
	regionRepository := repositories.NewRegionRepository(db)
	shipmentLineItemRepository := repositories.NewShipmentLineItemRepository(db)

	//services
	summaryService := services.NewSummaryService(summaryRepository, stockItemRepository)
	inventoryService := services.NewInventoryService(stockItemRepository, unitRepository, summaryService)
	carrierService := services.NewCarrierService(carrierRepository)
	stockLocationService := services.NewStockLocationService(stockLocationRepository)
	shippingMethodService := services.NewShippingMethodService(shippingMethodRepository)
	addressService := services.NewAddressService(addressRepository)
	regionService := services.NewRegionService(regionRepository)
	shipmentLineItemService := services.NewShipmentLineItemService(shipmentLineItemRepository)
	shipmentService := services.NewShipmentService(shipmentRepository, addressService, shipmentLineItemService)

	return map[string]controllers.IController{
		"/ping":             controllers.NewPingController(),
		"/summary":          controllers.NewSummaryController(summaryService),
		"/stock-items":      controllers.NewStockItemController(inventoryService),
		"/stock-locations":  controllers.NewStockLocationController(stockLocationService),
		"/reservations":     controllers.NewReservationController(inventoryService),
		"/carriers":         controllers.NewCarrierController(carrierService),
		"/shipping-methods": controllers.NewShippingMethodController(shippingMethodService),
		"/shipments":        controllers.NewShipmentController(shipmentService, addressService, regionService, shipmentLineItemService),
	}
}
