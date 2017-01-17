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
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type endToEndTestSuite struct {
	suite.Suite
	db       *gorm.DB
	router   *gin.Engine
	location *models.StockLocation
	server   *tests.TestWebServer
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
	})

	suite.location = fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(suite.location).Error)
}

func (suite *endToEndTestSuite) Test_HoldSKU() {
	skuPayload := fixtures.GetCreateSKUPayload()
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
