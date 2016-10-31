package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type summaryControllerTestSuite struct {
	GeneralControllerTestSuite
	service *mocks.SummaryServiceMock
}

func TestSummaryControllerSuite(t *testing.T) {
	suite.Run(t, new(summaryControllerTestSuite))
}

func (suite *summaryControllerTestSuite) SetupSuite() {
	// set up test env once
	suite.service = new(mocks.SummaryServiceMock)
	suite.router = gin.Default()

	controller := NewSummaryController(suite.service)
	controller.SetUp(suite.router.Group("/summary"))
}

func (suite *summaryControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *summaryControllerTestSuite) Test_GetSummary() {
	suite.service.On("GetSummary").Return([]*models.StockItemSummary{{
		StockItemID: 1,
		Type:        models.Sellable,
	}}, nil).Once()

	res := suite.Get("/summary")

	suite.Equal(http.StatusOK, res.Code)
	suite.Contains(res.Body.String(), "summary\":[")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKU() {
	suite.service.On("GetSummaryBySKU", uint(1)).Return([]*models.StockItemSummary{{
		StockItemID: 1,
		StockItem:   models.StockItem{},
		Type:        models.Sellable,
	}}, nil).Once()

	res := suite.Get("/summary/1")

	suite.Equal(http.StatusOK, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUNoSKU() {
	suite.service.On("GetSummaryBySKU", uint(32)).Return(nil, gorm.ErrRecordNotFound).Once()

	res := suite.Get("/summary/32")

	suite.Equal(http.StatusNotFound, res.Code)
	suite.Contains(res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUServerError() {
	suite.service.On("GetSummaryBySKU", uint(32)).Return(nil, gorm.ErrUnaddressable).Once()

	res := suite.Get("/summary/32")

	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}
