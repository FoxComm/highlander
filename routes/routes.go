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
	shippingMethodRepository := repositories.NewShippingMethodRepository(db)

	transactioner := repositories.NewDBTransactioner(db)

	//services
	summaryService := services.NewSummaryService(summaryRepository, stockItemRepository, transactioner)
	inventoryService := services.NewInventoryService(stockItemRepository, unitRepository, summaryService, transactioner)
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
