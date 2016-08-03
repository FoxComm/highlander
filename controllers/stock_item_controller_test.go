package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"

	"errors"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type stockItemControllerTestSuite struct {
	GeneralControllerTestSuite
	service *mocks.InventoryServiceMock
}

func TestStockItemControllerSuite(t *testing.T) {
	suite.Run(t, new(stockItemControllerTestSuite))
}

func (suite *stockItemControllerTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
	// set up test env once
	suite.service = new(mocks.InventoryServiceMock)
	suite.router = gin.Default()

	controller := NewStockItemController(suite.service)
	controller.SetUp(suite.router.Group("/stock-items"))
}

func (suite *stockItemControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *stockItemControllerTestSuite) Test_GetStockItems() {
	suite.service.On("GetStockItems").Return([]*models.StockItem{
		{
			SKU:             "SKU",
			StockLocationID: 1,
		},
	}, nil).Once()

	res := suite.Get("/stock-items/")

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Contains(res.Body.String(), `"sku":"SKU"`)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_GetStockItems_Error() {
	errText := "Some error"
	suite.service.On("GetStockItems").Return(nil, errors.New(errText)).Once()

	res := suite.Get("/stock-items/")

	suite.assert.Equal(http.StatusInternalServerError, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
	suite.assert.Contains(res.Body.String(), errText)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById() {
	suite.service.On("GetStockItemById", uint(1)).Return(&models.StockItem{
		SKU:             "SKU",
		StockLocationID: 1,
	}, nil).Once()

	res := suite.Get("/stock-items/1")

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Contains(res.Body.String(), `"sku":"SKU"`)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById_NotFound() {
	suite.service.On("GetStockItemById", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	res := suite.Get("/stock-items/1")

	suite.assert.Equal(http.StatusNotFound, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_GetStockItemById_WrongId() {
	res := suite.Get("/stock-items/abs")

	suite.assert.Equal(http.StatusBadRequest, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
}

func (suite *stockItemControllerTestSuite) Test_CreateStockItem() {
	stockItem := &models.StockItem{SKU: "SKU", StockLocationID: 1, DefaultUnitCost: 1000}

	suite.service.On("CreateStockItem", stockItem).Return(stockItem, nil).Once()

	var result models.StockItem
	jsonStr := `{"sku":"SKU","stockLocationID":1,"defaultUnitCost":1000}`
	res := suite.Post("/stock-items/", jsonStr, &result)

	suite.assert.Equal(http.StatusCreated, res.Code)
	suite.assert.Equal(stockItem.StockLocationID, result.StockLocationID)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_CreateStockItem_Error() {
	stockItem := &models.StockItem{SKU: "SKU", StockLocationID: 1, DefaultUnitCost: 1000}

	suite.service.On("CreateStockItem", stockItem).Return(nil, gorm.ErrInvalidTransaction).Once()

	jsonStr := `{"sku":"SKU","stockLocationID":1,"defaultUnitCost":1000}`
	res := suite.Post("/stock-items/", jsonStr)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits() {
	// couldn't find the way to set array of models to mock args expectation
	suite.service.On("IncrementStockItemUnits", uint(1), models.StockItemTypes().Sellable, mock.AnythingOfType("[]*models.StockItemUnit")).Return(nil).Once()

	jsonStr := `{"stockLocationId":1,"qty":1,"unit_cost":12000,"type":"Sellable","status":"onHand"}`
	res := suite.Patch("/stock-items/1/increment", jsonStr)

	suite.assert.Equal(http.StatusNoContent, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits_WrongId() {
	jsonStr := `{"stockLocationId":1,"qty": 1,"unit_cost": 12000,"type":"Sellable","status": "onHand"}`
	res := suite.Patch("/stock-items/asdasd/increment", jsonStr)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_IncrementStockItemUnits_WrongQty() {
	jsonStr := `{"stockLocationId":1,"qty": -1,"unit_cost": 12000,"type":"Sellable","status": "onHand"}`
	res := suite.Patch("/stock-items/1/increment", jsonStr)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits() {
	suite.service.On("DecrementStockItemUnits", uint(1), models.StockItemTypes().Sellable, 1).Return(nil).Once()

	jsonStr := `{"stockLocationId":1,"qty": 1,"type":"Sellable"}`
	res := suite.Patch("/stock-items/1/decrement", jsonStr)

	suite.assert.Equal(http.StatusNoContent, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits_WrongId() {
	jsonStr := `{"stockLocationId":1,"qty": 1,"type":"Sellable"}`
	res := suite.Patch("/stock-items/asdasd/decrement", jsonStr)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
}

func (suite *stockItemControllerTestSuite) Test_DecrementStockItemUnits_WrongQty() {
	jsonStr := `{"stockLocationId":1,"qty": -1,"type":"Sellable"}`
	res := suite.Patch("/stock-items/1/decrement", jsonStr)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
}
