package controllers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type summaryControllerTestSuite struct {
	suite.Suite
	assert  *assert.Assertions
	service *mocks.SummaryServiceMock
	router  *gin.Engine
}

func TestSummaryControllerSuite(t *testing.T) {
	suite.Run(t, new(summaryControllerTestSuite))
}

func (suite *summaryControllerTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
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
	suite.service.On("GetSummary").Return([]*models.StockItemSummary{
		{
			SKU:         "SKU",
			StockItemID: 0,
			OnHand:      0,
			OnHold:      0,
			Reserved:    0,
		},
	}, nil).Once()

	req, _ := http.NewRequest("GET", "/summary/", nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Contains(res.Body.String(), "counts\":[")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKU() {
	sku := "TEST-SKU"
	suite.service.On("GetSummaryBySKU", sku).Return(&models.StockItemSummary{
		SKU:         sku,
		StockItemID: 0,
		OnHand:      0,
		OnHold:      0,
		Reserved:    0,
	}, nil).Once()

	req, _ := http.NewRequest("GET", "/summary/"+sku, nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Contains(res.Body.String(), sku)
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUNoSKU() {
	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrRecordNotFound).Once()

	req, _ := http.NewRequest("GET", "/summary/NO-SKU", nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusNotFound, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) Test_GetSummaryBySKUServerError() {
	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrUnaddressable).Once()

	req, _ := http.NewRequest("GET", "/summary/NO-SKU", nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}
