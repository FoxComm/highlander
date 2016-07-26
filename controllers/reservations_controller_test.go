package controllers

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/middlewarehouse/controllers/mocks"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type reservationControllerTestSuite struct {
	suite.Suite
	assert  *assert.Assertions
	service *mocks.InventoryServiceMock
	router  *gin.Engine
}

func TestReservationControllerSuite(t *testing.T) {
	suite.Run(t, new(reservationControllerTestSuite))
}

func (suite *reservationControllerTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
	// set up test env once
	suite.service = new(mocks.InventoryServiceMock)
	suite.router = gin.Default()

	suite.service = new(mocks.InventoryServiceMock)

	controller := NewReservationController(suite.service)
	controller.SetUp(suite.router.Group("/reservations"))
}

func (suite *reservationControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *reservationControllerTestSuite) TestReserveItems() {
	suite.service.On("ReserveItems", "BR10001", map[string]int{"SKU": 2}).Return(nil).Once()

	jsonStr := []byte(`{"refNum": "BR10001","reservations": [{ "sku": "SKU", "qty": 2 }]}`)

	req, _ := http.NewRequest("POST", "/reservations/reserve", bytes.NewBuffer(jsonStr))
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Equal("{}\n", res.Body.String())

	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) TestReserveItemsWrongSKUs() {
	suite.service.On("ReserveItems", "BR10001", map[string]int{"SKU": 2}).Return(gorm.ErrRecordNotFound).Once()

	jsonStr := []byte(`{"refNum": "BR10001","reservations": [{ "sku": "SKU", "qty": 2 }]}`)

	req, _ := http.NewRequest("POST", "/reservations/reserve", bytes.NewBuffer(jsonStr))
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusNotFound, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")

	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) TestReserveItemsEmptySKUsList() {
	jsonStr := []byte(`{"refNum": "BR10001","reservations": []}`)

	req, _ := http.NewRequest("POST", "/reservations/reserve", bytes.NewBuffer(jsonStr))
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusBadRequest, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")
	suite.assert.Contains(res.Body.String(), "Reservation must have at least one SKU")
}

func (suite *reservationControllerTestSuite) TestReleaseItems() {
	suite.service.On("ReleaseItems", "BR10001").Return(nil).Once()

	jsonStr := []byte(`{"refNum": "BR10001"}`)

	req, _ := http.NewRequest("POST", "/reservations/cancel", bytes.NewBuffer(jsonStr))
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Equal("{}\n", res.Body.String())

	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) TestReserveItemsWrongRefNum() {
	suite.service.On("ReleaseItems", "BR10001").Return(gorm.ErrRecordNotFound).Once()

	jsonStr := []byte(`{"refNum": "BR10001"}`)

	req, _ := http.NewRequest("POST", "/reservations/cancel", bytes.NewBuffer(jsonStr))
	res := httptest.NewRecorder()
	suite.router.ServeHTTP(res, req)

	suite.assert.Equal(http.StatusNotFound, res.Code)
	suite.assert.Contains(res.Body.String(), "errors")

	suite.service.AssertExpectations(suite.T())
}
