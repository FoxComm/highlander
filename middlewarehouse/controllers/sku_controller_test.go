package controllers

import (
	"encoding/json"
	"fmt"
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

type skuControllerTestSuite struct {
	GeneralControllerTestSuite
	db        *gorm.DB
	location  *models.StockLocation
	sku       *models.SKU
	stockItem *models.StockItem
}

func TestSKUControllerTestSuite(t *testing.T) {
	suite.Run(t, new(skuControllerTestSuite))
}

func (suite *skuControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()
	controller := NewSKUController(suite.db)
	controller.SetUp(suite.router.Group("/skus"))
}

func (suite *skuControllerTestSuite) SetupTest() {
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

	suite.sku = fixtures.GetSKU()
	suite.Nil(suite.db.Create(suite.sku).Error)

	suite.stockItem = &models.StockItem{
		SKU:             suite.sku.Code,
		StockLocationID: suite.location.ID,
	}
	suite.Nil(suite.db.Create(suite.stockItem).Error)
}

func (suite *skuControllerTestSuite) Test_GetSKU_Success() {
	res := suite.Get("/skus/1")
	suite.Equal(http.StatusOK, res.Code)

	respBody := new(responses.SKU)
	err := json.NewDecoder(res.Body).Decode(respBody)
	suite.Nil(err)
	suite.Equal(suite.sku.ID, respBody.ID)
	suite.Equal(suite.sku.Code, respBody.Code)
}

func (suite *skuControllerTestSuite) Test_CreateSKU_Success() {
	payload := fixtures.GetCreateSKUPayload()
	res := suite.Post("/skus", payload)
	suite.Equal(http.StatusCreated, res.Code)

	respBody := new(responses.SKU)
	err := json.NewDecoder(res.Body).Decode(respBody)
	suite.Nil(err)
	suite.Equal(payload.Code, respBody.Code)
	suite.Equal(payload.UPC, respBody.UPC)
}

func (suite *skuControllerTestSuite) Test_CreateSKU_CreatesStockItem() {
	payload := fixtures.GetCreateSKUPayload()
	payload.RequiresInventoryTracking = true
	res := suite.Post("/skus", payload)
	suite.Equal(http.StatusCreated, res.Code)

	var stockItems []*models.StockItem
	suite.Nil(suite.db.Where("sku = ?", payload.Code).Find(&stockItems).Error)
	suite.Equal(1, len(stockItems))
}

func (suite *skuControllerTestSuite) Test_CreateSKU_CreatesStockItemSummary() {
	payload := fixtures.GetCreateSKUPayload()
	payload.RequiresInventoryTracking = true
	res := suite.Post("/skus", payload)
	suite.Equal(http.StatusCreated, res.Code)

	var stockItem models.StockItem
	suite.Nil(suite.db.Where("sku = ?", payload.Code).First(&stockItem).Error)

	var summaries []*models.StockItemSummary
	suite.Nil(suite.db.Where("stock_item_id = ?", stockItem.ID).Find(&summaries).Error)
	suite.Equal(4, len(summaries))
}

func (suite *skuControllerTestSuite) Test_UpdateSKUCode_Success() {
	code := "UPDATED"
	payload := &payloads.UpdateSKU{Code: &code}

	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
	res := suite.Patch(url, payload)
	suite.Equal(http.StatusOK, res.Code)
}

func (suite *skuControllerTestSuite) Test_UpdateSKUCodeAndRequest_Success() {
	code := "UPDATED"
	payload := &payloads.UpdateSKU{Code: &code}

	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
	res := suite.Patch(url, payload)
	suite.Equal(http.StatusOK, res.Code)

	getRes := suite.Get(url)
	suite.Equal(http.StatusOK, getRes.Code)

	respBody := new(responses.SKU)
	err := json.NewDecoder(getRes.Body).Decode(respBody)
	suite.Nil(err)
	suite.Equal(suite.sku.ID, respBody.ID)
	suite.Equal(code, respBody.Code)
}

func (suite *skuControllerTestSuite) Test_UpdateSKUCodeBlank_Failure() {
	code := ""
	payload := &payloads.UpdateSKU{Code: &code}

	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
	res := suite.Patch(url, payload)
	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *skuControllerTestSuite) Test_DeleteSKU_Success() {
	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
	res := suite.Delete(url)
	suite.Equal(http.StatusNoContent, res.Code)
}
