package main

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/common/tests"
	"github.com/FoxComm/highlander/middlewarehouse/controllers"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type endToEndTestSuite struct {
	suite.Suite
	db             *gorm.DB
	router         *gin.Engine
	location       *models.StockLocation
	shippingMethod *models.ShippingMethod
	server         *tests.TestWebServer
}

func TestEndToEndSuite(t *testing.T) {
	suite.Run(t, new(endToEndTestSuite))
}

func (suite *endToEndTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	skuController := controllers.NewSKUController(suite.db)
	skuController.SetUp(suite.router.Group("/skus"))

	inventoryService := services.NewInventoryService(suite.db)
	stockItemController := controllers.NewStockItemController(inventoryService)
	stockItemController.SetUp(suite.router.Group("/stock-items"))

	reservationController := controllers.NewReservationController(inventoryService)
	reservationController.SetUp(suite.router.Group("/reservations"))

	summaryService := services.NewSummaryService(suite.db)
	summaryController := controllers.NewSummaryController(summaryService)
	summaryController.SetUp(suite.router.Group("/summary"))

	shipmentService := services.NewShipmentService(suite.db, inventoryService, summaryService, dummyLogger{})
	shipmentController := controllers.NewShipmentController(shipmentService)
	shipmentController.SetUp(suite.router.Group("/shipments"))

	suite.server = &tests.TestWebServer{Router: suite.router}
}

func (suite *endToEndTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"inventory_search_view",
		"skus",
		"stock_items",
		"stock_item_units",
		"stock_item_summaries",
		"stock_locations",
		"shipments",
		"carriers",
		"shipping_methods",
	})

	carrier := fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(carrier).Error)

	suite.shippingMethod = fixtures.GetShippingMethod(uint(0), carrier.ID, carrier)
	suite.Nil(suite.db.Create(suite.shippingMethod).Error)

	suite.location = fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(suite.location).Error)
}

func (suite *endToEndTestSuite) Test_HoldSKU() {
	skuPayload := fixtures.GetCreateSKUPayload()
	skuPayload.RequiresInventoryTracking = true
	skuRes := suite.server.Post("/skus", skuPayload)
	suite.Equal(http.StatusCreated, skuRes.Code)

	var stockItem models.StockItem
	suite.Nil(suite.db.Where("sku = ?", skuPayload.Code).First(&stockItem).Error)

	incrementURL := fmt.Sprintf("/stock-items/%d/increment", stockItem.ID)
	incrementPayload := payloads.IncrementStockItemUnits{
		Qty:    10,
		Status: "onHand",
		Type:   "Sellable",
	}
	incrementRes := suite.server.Patch(incrementURL, incrementPayload)
	suite.Equal(http.StatusNoContent, incrementRes.Code)

	summaryURL := fmt.Sprintf("/summary/%s", skuPayload.Code)
	var summaryResponse responses.StockItemSummary
	summaryRes := suite.server.Get(summaryURL, &summaryResponse)
	suite.Equal(http.StatusOK, summaryRes.Code)

	for _, summary := range summaryResponse.Summary {
		suite.Equal(skuPayload.Code, summary.SKU)
		suite.Equal(0, summary.OnHold)
		suite.Equal(0, summary.Reserved)
		suite.Equal(0, summary.Shipped)

		switch summary.Type {
		case "Sellable":
			suite.Equal(10, summary.OnHand)
			suite.Equal(10, summary.AFS)
			suite.Equal(skuPayload.UnitCost.Value*10, summary.AFSCost)
		default:
			suite.Equal(0, summary.OnHand)
			suite.Equal(0, summary.AFS)
			suite.Equal(0, summary.AFSCost)
		}
	}

	reservationPayload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: skuPayload.Code,
			},
		},
	}
	reservationRes := suite.server.Post("/reservations/hold", reservationPayload)
	suite.Equal(http.StatusNoContent, reservationRes.Code)

	summaryRes = suite.server.Get(summaryURL, &summaryResponse)
	suite.Equal(http.StatusOK, summaryRes.Code)

	for _, summary := range summaryResponse.Summary {
		suite.Equal(skuPayload.Code, summary.SKU)
		suite.Equal(0, summary.Reserved)
		suite.Equal(0, summary.Shipped)

		switch summary.Type {
		case "Sellable":
			suite.Equal(2, summary.OnHold)
			suite.Equal(10, summary.OnHand)
			suite.Equal(8, summary.AFS)
			suite.Equal(skuPayload.UnitCost.Value*8, summary.AFSCost)
		default:
			suite.Equal(0, summary.OnHold)
			suite.Equal(0, summary.OnHand)
			suite.Equal(0, summary.AFS)
			suite.Equal(0, summary.AFSCost)
		}
	}
}

func (suite *endToEndTestSuite) Test_HoldSKU_NoInventoryTracking() {
	skuPayload := fixtures.GetCreateSKUPayload()
	skuPayload.RequiresInventoryTracking = false
	skuRes := suite.server.Post("/skus", skuPayload)
	suite.Equal(http.StatusCreated, skuRes.Code)

	var stockItems []*models.StockItem
	suite.Nil(suite.db.Where("sku = ?", skuPayload.Code).Find(&stockItems).Error)
	suite.Equal(0, len(stockItems))

	reservationPayload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: skuPayload.Code,
			},
		},
	}
	reservationRes := suite.server.Post("/reservations/hold", reservationPayload)
	suite.Equal(http.StatusNoContent, reservationRes.Code)
}

func (suite *endToEndTestSuite) Test_HoldSKU_MixedInventoryTracking() {
	skuPayload1 := fixtures.GetCreateSKUPayload()
	skuPayload1.RequiresInventoryTracking = true
	skuRes := suite.server.Post("/skus", skuPayload1)
	suite.Equal(http.StatusCreated, skuRes.Code)

	skuPayload2 := fixtures.GetCreateSKUPayload()
	skuPayload2.RequiresInventoryTracking = false
	skuRes = suite.server.Post("/skus", skuPayload2)
	suite.Equal(http.StatusCreated, skuRes.Code)

	var stockItem1 models.StockItem
	suite.Nil(suite.db.Where("sku = ?", skuPayload1.Code).First(&stockItem1).Error)

	incrementURL := fmt.Sprintf("/stock-items/%d/increment", stockItem1.ID)
	incrementPayload := payloads.IncrementStockItemUnits{
		Qty:    10,
		Status: "onHand",
		Type:   "Sellable",
	}
	incrementRes := suite.server.Patch(incrementURL, incrementPayload)
	suite.Equal(http.StatusNoContent, incrementRes.Code)

	reservationPayload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: skuPayload1.Code,
			},
			payloads.ItemReservation{
				Qty: 10,
				SKU: skuPayload2.Code,
			},
		},
	}

	reservationRes := suite.server.Post("/reservations/hold", reservationPayload)
	suite.Equal(http.StatusNoContent, reservationRes.Code)

	summaryURL := fmt.Sprintf("/summary/%s", skuPayload1.Code)
	var summaryResponse responses.StockItemSummary
	summaryRes := suite.server.Get(summaryURL, &summaryResponse)
	suite.Equal(http.StatusOK, summaryRes.Code)

	for _, summary := range summaryResponse.Summary {
		suite.Equal(skuPayload1.Code, summary.SKU)
		suite.Equal(0, summary.Reserved)
		suite.Equal(0, summary.Shipped)

		switch summary.Type {
		case "Sellable":
			suite.Equal(2, summary.OnHold)
			suite.Equal(10, summary.OnHand)
			suite.Equal(8, summary.AFS)
			suite.Equal(skuPayload1.UnitCost.Value*8, summary.AFSCost)
		default:
			suite.Equal(0, summary.OnHold)
			suite.Equal(0, summary.OnHand)
			suite.Equal(0, summary.AFS)
			suite.Equal(0, summary.AFSCost)
		}
	}
}

func (suite *endToEndTestSuite) Test_CreateShipment_MixedInventoryTracking() {
	skuPayload1 := fixtures.GetCreateSKUPayload()
	skuPayload1.RequiresInventoryTracking = true
	skuRes := suite.server.Post("/skus", skuPayload1)
	suite.Equal(http.StatusCreated, skuRes.Code)

	skuPayload2 := fixtures.GetCreateSKUPayload()
	skuPayload2.RequiresInventoryTracking = false
	skuRes = suite.server.Post("/skus", skuPayload2)
	suite.Equal(http.StatusCreated, skuRes.Code)

	var stockItem1 models.StockItem
	suite.Nil(suite.db.Where("sku = ?", skuPayload1.Code).First(&stockItem1).Error)

	incrementURL := fmt.Sprintf("/stock-items/%d/increment", stockItem1.ID)
	incrementPayload := payloads.IncrementStockItemUnits{
		Qty:    10,
		Status: "onHand",
		Type:   "Sellable",
	}
	incrementRes := suite.server.Patch(incrementURL, incrementPayload)
	suite.Equal(http.StatusNoContent, incrementRes.Code)

	reservationPayload := payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{
				Qty: 2,
				SKU: skuPayload1.Code,
			},
			payloads.ItemReservation{
				Qty: 1,
				SKU: skuPayload2.Code,
			},
		},
	}

	reservationRes := suite.server.Post("/reservations/hold", reservationPayload)
	suite.Equal(http.StatusNoContent, reservationRes.Code)

	summaryURL := fmt.Sprintf("/summary/%s", skuPayload1.Code)
	summaryRes := suite.server.Get(summaryURL)
	suite.Equal(http.StatusOK, summaryRes.Code)

	order := fixtures.GetOrder("BR10004", 0)
	order.LineItems.SKUs = []payloads.OrderLineItem{
		payloads.OrderLineItem{
			SKU:              skuPayload1.Code,
			Name:             "Some name",
			Price:            5999,
			State:            "pending",
			ReferenceNumbers: []string{"abc"},
			ImagePath:        "test.com/test.png",
		},
		payloads.OrderLineItem{
			SKU:              skuPayload1.Code,
			Name:             "Some name",
			Price:            5999,
			State:            "pending",
			ReferenceNumbers: []string{"def"},
			ImagePath:        "test.com/test.png",
		},
		payloads.OrderLineItem{
			SKU:              skuPayload2.Code,
			Name:             "Another name",
			Price:            2500,
			State:            "pending",
			ReferenceNumbers: []string{"hig"},
			ImagePath:        "test.com/test.png",
		},
	}
	order.ShippingMethod = &payloads.OrderShippingMethod{
		ID:        suite.shippingMethod.ID,
		Name:      suite.shippingMethod.Name,
		Code:      suite.shippingMethod.Code,
		Price:     int(suite.shippingMethod.Cost),
		IsEnabled: true,
	}

	var shipmentResponse responses.Shipment
	shipmentRes := suite.server.Post("/shipments/from-order", order, &shipmentResponse)
	suite.Equal(http.StatusCreated, shipmentRes.Code)

	fmt.Printf("%s\n", shipmentRes.Body.String())
}

type dummyLogger struct{}

func (d dummyLogger) Log(activity activities.ISiteActivity) error { return nil }
