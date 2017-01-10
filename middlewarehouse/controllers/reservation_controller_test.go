package controllers

// import (
// 	"net/http"
// 	"testing"

// 	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"

// 	"github.com/gin-gonic/gin"
// 	"github.com/jinzhu/gorm"
// 	"github.com/stretchr/testify/mock"
// 	"github.com/stretchr/testify/suite"
// )

// type reservationControllerTestSuite struct {
// 	GeneralControllerTestSuite
// 	service *mocks.InventoryServiceMock
// }

// func TestReservationControllerSuite(t *testing.T) {
// 	suite.Run(t, new(reservationControllerTestSuite))
// }

// func (suite *reservationControllerTestSuite) SetupSuite() {
// 	// set up test env once
// 	suite.service = new(mocks.InventoryServiceMock)
// 	suite.router = gin.Default()

// 	controller := NewReservationController(suite.service)
// 	controller.SetUp(suite.router.Group("/reservations"))
// }

// func (suite *reservationControllerTestSuite) TearDownTest() {
// 	// clear service mock calls expectations after each test
// 	suite.service.ExpectedCalls = []*mock.Call{}
// 	suite.service.Calls = []mock.Call{}
// }

// func (suite *reservationControllerTestSuite) Test_ReserveItems() {
// 	suite.service.On("HoldItems", "BR10001", map[string]int{"SKU": 2}).Return(nil).Once()

// 	jsonStr := `{"refNum":"BR10001","items":[{ "sku": "SKU", "qty": 2 }]}`

// 	res := suite.Post("/reservations/hold", jsonStr)

// 	suite.Equal(http.StatusNoContent, res.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongSKUs() {
// 	suite.service.On("HoldItems", "BR10001", map[string]int{"SKU": 2}).Return(gorm.ErrRecordNotFound).Once()

// 	jsonStr := `{"refNum": "BR10001","items": [{ "sku": "SKU", "qty": 2 }]}`

// 	res := suite.Post("/reservations/hold", jsonStr)

// 	suite.Equal(http.StatusNotFound, res.Code)
// 	suite.Contains(res.Body.String(), "errors")

// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *reservationControllerTestSuite) Test_ReserveItems_EmptySKUsList() {
// 	jsonStr := `{"refNum": "BR10001","items": []}`

// 	res := suite.Post("/reservations/hold", jsonStr)

// 	suite.Equal(http.StatusBadRequest, res.Code)
// 	suite.Contains(res.Body.String(), "errors")
// 	suite.Contains(res.Body.String(), "Reservation must have at least one SKU")
// }

// func (suite *reservationControllerTestSuite) Test_ReleaseItems() {
// 	suite.service.On("ReleaseItems", "BR10001").Return(nil).Once()

// 	res := suite.Delete("/reservations/hold/BR10001")

// 	suite.Equal(http.StatusNoContent, res.Code)
// 	suite.service.AssertExpectations(suite.T())
// }

// func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongRefNum() {
// 	suite.service.On("ReleaseItems", "BR10001").Return(gorm.ErrRecordNotFound).Once()

// 	res := suite.Delete("/reservations/hold/BR10001")

// 	suite.Equal(http.StatusNotFound, res.Code)
// 	suite.Contains(res.Body.String(), "errors")

// 	suite.service.AssertExpectations(suite.T())
// }
