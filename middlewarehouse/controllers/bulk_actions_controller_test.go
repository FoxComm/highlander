package controllers

import (
	"encoding/json"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type bulkActionsControllerTestSuite struct {
	GeneralControllerTestSuite
	db       *gorm.DB
	location *models.StockLocation
}

func TestBulkActionsControllerTestSuite(t *testing.T) {
	suite.Run(t, new(bulkActionsControllerTestSuite))
}

func (suite *bulkActionsControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()
	controller := NewBulkActionsController(suite.db)
	controller.SetUp(suite.router.Group("/bulk"))
}

func (suite *bulkActionsControllerTestSuite) SetupTest() {
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

func (suite *bulkActionsControllerTestSuite) Test_CreateSKU_Success() {
	payload := []*payloads.CreateSKU{
		fixtures.GetCreateSKUPayload(),
		fixtures.GetCreateSKUPayload(),
	}

	res := suite.Post("/bulk/skus", payload)
	suite.Equal(http.StatusCreated, res.Code)

	respBody := []responses.SKU{}
	err := json.NewDecoder(res.Body).Decode(&respBody)
	suite.Nil(err)
	suite.Equal(2, len(respBody))
}
