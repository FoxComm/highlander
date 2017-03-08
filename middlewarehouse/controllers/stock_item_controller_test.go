package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
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

type stockItemControllerTestSuite struct {
	GeneralControllerTestSuite
	db            *gorm.DB
	stockLocation *models.StockLocation
	stockItem     *models.StockItem
	summary       *models.StockItemSummary
}

func TestStockItemControllerSuite(t *testing.T) {
	suite.Run(t, new(stockItemControllerTestSuite))
}

func (suite *stockItemControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	inventoryService := services.NewInventoryService(suite.db)

	controller := NewStockItemController(inventoryService)
	controller.SetUp(suite.router.Group("/stock-items"))

	tasks.TruncateTables(suite.db, []string{"stock_locations"})

	suite.stockLocation = fixtures.GetStockLocation()
	suite.Nil(suite.db.Create(suite.stockLocation).Error)

}

func (suite *stockItemControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"inventory_search_view",
		"stock_items",
		"stock_item_summaries",
		"stock_item_units",
	})

	suite.stockItem = &models.StockItem{
		SKU:             "SKU",
		StockLocationID: suite.stockLocation.ID,
	}
	suite.Nil(suite.db.Create(suite.stockItem).Error)

	suite.summary = &models.StockItemSummary{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
	}
	suite.Nil(suite.db.Create(suite.summary).Error)
}

func (suite *stockItemControllerTestSuite) Test_GetStockItems() {
	result := []responses.StockItem{}
	res := suite.Get("/stock-items", &result)

	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(1, len(result))
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById() {
	url := fmt.Sprintf("/stock-items/%d", suite.stockItem.ID)
	result := responses.StockItem{}
	res := suite.Get(url, &result)

	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(suite.stockItem.SKU, result.SKU)
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById_NotFound() {
	res := suite.Get("/stock-items/100")

	suite.Equal(http.StatusNotFound, res.Code)
	suite.Contains(res.Body.String(), "errors")
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById_WrongId() {
	res := suite.Get("/stock-items/abs")

	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
}

func (suite *stockItemControllerTestSuite) Test_CreateStockItem() {
	payload := payloads.StockItem{
		SKU:             "SKU-NEW",
		StockLocationID: suite.stockLocation.ID,
		DefaultUnitCost: 999,
	}

	var result responses.StockItem
	res := suite.Post("/stock-items", payload, &result)

	suite.Equal(http.StatusCreated, res.Code)
	suite.Equal(payload.StockLocationID, result.StockLocationID)
}

func (suite *stockItemControllerTestSuite) Test_CreateStockItem_Error() {
	payload := payloads.StockItem{
		SKU:             "SKU",
		StockLocationID: 999,
		DefaultUnitCost: 999,
	}

	var result responses.StockItem
	res := suite.Post("/stock-items", payload, &result)

	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits() {
	payload := payloads.IncrementStockItemUnits{
		Qty:      1,
		UnitCost: 12000,
		Status:   "onHand",
		Type:     "Sellable",
	}

	url := fmt.Sprintf("/stock-items/%d/increment", suite.stockItem.ID)
	res := suite.Patch(url, payload)

	suite.Equal(http.StatusNoContent, res.Code)

	units := []*models.StockItemUnit{}
	suite.Nil(suite.db.Where("stock_item_id = ?", suite.stockItem.ID).Find(&units).Error)
	suite.Equal(1, len(units))
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits_WrongId() {
	payload := payloads.IncrementStockItemUnits{
		Qty:      1,
		UnitCost: 12000,
		Status:   "onHand",
		Type:     "Sellable",
	}
	res := suite.Patch("/stock-items/asdasd/increment", payload)

	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits_WrongQty() {
	payload := payloads.IncrementStockItemUnits{
		Qty:      -1,
		UnitCost: 12000,
		Status:   "onHand",
		Type:     "Sellable",
	}
	url := fmt.Sprintf("/stock-items/%d/increment", suite.stockItem.ID)
	res := suite.Patch(url, payload)

	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits() {
	stockItemUnit := models.StockItemUnit{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
		Status:      models.StatusOnHand,
	}
	suite.Nil(suite.db.Create(&stockItemUnit).Error)

	payload := payloads.DecrementStockItemUnits{
		Qty:  1,
		Type: "Sellable",
	}

	url := fmt.Sprintf("/stock-items/%d/decrement", suite.stockItem.ID)
	res := suite.Patch(url, payload)

	suite.Equal(http.StatusNoContent, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits_WrongId() {
	payload := payloads.DecrementStockItemUnits{Qty: 1, Type: "Sellable"}
	res := suite.Patch("/stock-items/asdasd/decrement", payload)
	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits_WrongQty() {
	payload := payloads.DecrementStockItemUnits{Qty: -1, Type: "Sellable"}
	url := fmt.Sprintf("/stock-items/%d/decrement", suite.stockItem.ID)
	res := suite.Patch(url, payload)

	suite.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_GetAFS_ById() {
	stockItemUnit := models.StockItemUnit{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
		Status:      models.StatusOnHand,
	}
	suite.Nil(suite.db.Create(&stockItemUnit).Error)

	var summary models.StockItemSummary
	err := suite.db.
		Where("stock_item_id = ?", suite.stockItem.ID).
		Where("type = ?", models.Sellable).
		First(&summary).
		Error
	suite.Nil(err)

	summary.OnHand = 1
	summary.AFS = 1
	suite.Nil(suite.db.Save(&summary).Error)

	var result responses.AFS
	url := fmt.Sprintf("/stock-items/%d/afs/Sellable", suite.stockItem.ID)

	res := suite.Get(url, &result)
	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(1, result.AFS)
}

func (suite *stockItemControllerTestSuite) Test_GetAFS_BySKU() {
	stockItemUnit := models.StockItemUnit{
		StockItemID: suite.stockItem.ID,
		Type:        models.Sellable,
		Status:      models.StatusOnHand,
	}
	suite.Nil(suite.db.Create(&stockItemUnit).Error)

	var summary models.StockItemSummary
	err := suite.db.
		Where("stock_item_id = ?", suite.stockItem.ID).
		Where("type = ?", models.Sellable).
		First(&summary).
		Error
	suite.Nil(err)

	summary.OnHand = 1
	summary.AFS = 1
	suite.Nil(suite.db.Save(&summary).Error)

	var result responses.AFS
	url := fmt.Sprintf("/stock-items/%s/afs/Sellable", suite.stockItem.SKU)

	res := suite.Get(url, &result)
	suite.Equal(http.StatusOK, res.Code)
	suite.Equal(1, result.AFS)
}
