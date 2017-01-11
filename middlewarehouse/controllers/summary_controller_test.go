package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type summaryControllerTestSuite struct {
	GeneralControllerTestSuite
	db        *gorm.DB
	service   services.SummaryService
	stockItem *models.StockItem
}

func TestSummaryControllerSuite(t *testing.T) {
	suite.Run(t, new(summaryControllerTestSuite))
}

func (suite *summaryControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	suite.service = services.NewSummaryService(suite.db)

	controller := NewSummaryController(suite.service)
	controller.SetUp(suite.router.Group("/summary"))
}

func (suite *summaryControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"inventory_search_view",
		"stock_items",
		"stock_locations",
		"stock_item_summaries",
	})

	stockLocation := fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(stockLocation).Error)

	suite.stockItem = fixtures.GetStockItem(stockLocation.ID, "TEST-SKU")
	suite.Nil(suite.db.Create(suite.stockItem).Error)
}

func (suite *summaryControllerTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *summaryControllerTestSuite) Test_GetSummary() {
	summary := models.StockItemSummary{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
		OnHand:      13,
	}
	suite.Nil(suite.db.Create(&summary).Error)

	var response struct {
		Summary []interface{}
	}

	res := suite.Get("/summary", &response)
	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(1, len(response.Summary))
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKU() {
	sku := "TEST-SKU"
	summary := models.StockItemSummary{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
		OnHand:      13,
	}
	suite.Nil(suite.db.Create(&summary).Error)

	var summaryResp responses.StockItemSummary
	res := suite.Get(fmt.Sprintf("/summary/%s", sku), &summaryResp)

	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(1, len(summaryResp.Summary))
	suite.Equal(sku, summaryResp.Summary[0].SKU)
	suite.Equal(13, summaryResp.Summary[0].OnHand)
}

// func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUNoSKU() {
// 	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrRecordNotFound).Once()

// 	res := suite.Get("/summary/NO-SKU")

// 	suite.Equal(http.StatusNotFound, res.Code)
// 	suite.Contains(res.Body.String(), "errors")
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUServerError() {
// 	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrUnaddressable).Once()

// 	res := suite.Get("/summary/NO-SKU")

// 	suite.Equal(http.StatusBadRequest, res.Code)
// 	suite.Contains(res.Body.String(), "errors")
// 	suite.service.AssertExpectations(suite.T())
// }
