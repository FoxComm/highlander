package controllers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

// SummaryService Mock
type summaryServiceMock struct {
	mock.Mock
}

type summaryControllerTestSuite struct {
	suite.Suite
	service *summaryServiceMock
	router  *gin.Engine
}

func TestSummaryControllerSuite(t *testing.T) {
	suite.Run(t, new(summaryControllerTestSuite))
}

func (suite *summaryControllerTestSuite) SetupSuite() {
	// set up test env once
	suite.service = new(summaryServiceMock)
	suite.router = gin.Default()

	controller := NewSummaryController(suite.service)
	controller.SetUp(suite.router.Group("/summary"))
}

func (suite *summaryControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *summaryControllerTestSuite) TestGetSummaries() {
	suite.service.On("GetSummaries").Return([]*models.StockItemSummary{
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

	assert.Equal(suite.T(), http.StatusOK, res.Code)
	assert.Contains(suite.T(), res.Body.String(), "counts\":[")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) TestGetSummaryBySKU() {
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

	assert.Equal(suite.T(), http.StatusOK, res.Code)
	assert.Contains(suite.T(), res.Body.String(), sku)
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) TestGetSummaryBySKUNoSKU() {
	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrRecordNotFound).Once()

	req, _ := http.NewRequest("GET", "/summary/NO-SKU", nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	assert.Equal(suite.T(), http.StatusNotFound, res.Code)
	assert.Contains(suite.T(), res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}

func (suite *summaryControllerTestSuite) TestGetSummaryBySKUServerError() {
	suite.service.On("GetSummaryBySKU", "NO-SKU").Return(nil, gorm.ErrUnaddressable).Once()

	req, _ := http.NewRequest("GET", "/summary/NO-SKU", nil)
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	assert.Equal(suite.T(), http.StatusBadRequest, res.Code)
	assert.Contains(suite.T(), res.Body.String(), "errors")
	suite.service.AssertExpectations(suite.T())
}

// implement ISummaryService interface to pass mock as service (another solution?)
func (m summaryServiceMock) GetSummaries() ([]*models.StockItemSummary, error) {
	args := m.Called()

	if model, ok := args.Get(0).([]*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m summaryServiceMock) GetSummaryBySKU(code string) (*models.StockItemSummary, error) {
	args := m.Called(code)

	if model, ok := args.Get(0).(*models.StockItemSummary); ok {
		return model, nil
	}

	return nil, args.Error(1)
}

func (m summaryServiceMock) CreateStockItemSummary(stockItemId uint, dbContext *gorm.DB) error {
	return nil
}

func (m summaryServiceMock) UpdateStockItemSummary(stockItemID uint, qty int, status services.StatusChange, dbContext *gorm.DB) error {
	return nil
}
