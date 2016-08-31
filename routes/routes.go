package routes

import (
	"log"

	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/common/config"
	"github.com/FoxComm/middlewarehouse/controllers"
	"github.com/FoxComm/middlewarehouse/repositories"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/jinzhu/gorm"
)

func GetRoutes(db *gorm.DB) map[string]controllers.IController {
	// Kkkkkafka
	broker := config.Config.KafkaBroker
	schemaRegistryURL := config.Config.SchemaRegistryURL

	producer, err := metamorphosis.NewProducer(broker, schemaRegistryURL)
	if err != nil {
		log.Panicf("Unable to initialize Kafka producer with error %s", err.Error())
	}

	//repositories
	carrierRepository := repositories.NewCarrierRepository(db)
	summaryRepository := repositories.NewSummaryRepository(db)
	stockItemRepository := repositories.NewStockItemRepository(db)
	unitRepository := repositories.NewStockItemUnitRepository(db)
	stockLocationRepository := repositories.NewStockLocationRepository(db)
	shippingMethodRepository := repositories.NewShippingMethodRepository(db)

	//services
	summaryService := services.NewSummaryService(summaryRepository, stockItemRepository)
	inventoryService := services.NewInventoryService(stockItemRepository, unitRepository, summaryService)
	carrierService := services.NewCarrierService(carrierRepository)
	stockLocationService := services.NewStockLocationService(stockLocationRepository)
	shippingMethodService := services.NewShippingMethodService(shippingMethodRepository)
	shipmentService := services.NewShipmentService(db, summaryService)
	activityLogger := services.NewActivityLogger(producer)

	return map[string]controllers.IController{
		"v1/public/ping":             controllers.NewPingController(),
		"v1/public/summary":          controllers.NewSummaryController(summaryService),
		"v1/public/stock-items":      controllers.NewStockItemController(inventoryService),
		"v1/public/stock-locations":  controllers.NewStockLocationController(stockLocationService),
		"v1/public/carriers":         controllers.NewCarrierController(carrierService),
		"v1/public/shipping-methods": controllers.NewShippingMethodController(shippingMethodService),
		"v1/public/shipments":        controllers.NewShipmentController(shipmentService, activityLogger),

		"v1/private/reservations": controllers.NewReservationController(inventoryService),
	}
}
