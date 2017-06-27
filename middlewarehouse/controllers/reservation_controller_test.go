package controllers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	commonErrors "github.com/FoxComm/highlander/middlewarehouse/common/errors"
	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"

	"errors"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type reservationControllerTestSuite struct {
	GeneralControllerTestSuite
	service *mocks.InventoryServiceMock
}

func TestReservationControllerSuite(t *testing.T) {
	suite.Run(t, new(reservationControllerTestSuite))
}

func (suite *reservationControllerTestSuite) SetupSuite() {
	// set up test env once
	suite.service = new(mocks.InventoryServiceMock)
	suite.router = gin.Default()

	controller := NewReservationController(suite.service)
	controller.SetUp(suite.router.Group("/reservations"))
}

func (suite *reservationControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func reserveItemTest(suite *reservationControllerTestSuite, holdItemsResult interface{}) *httptest.ResponseRecorder {
	payload := &payloads.Reservation{
		RefNum: "BR10001",
		Items: []payloads.ItemReservation{
			payloads.ItemReservation{SKU: "SKU", Qty: 2},
		},
		Scopable: payloads.Scopable{
			Scope: "1",
		},
	}
	suite.service.On("HoldItems", payload).Return(holdItemsResult).Once()

	jsonStr := `{"refNum":"BR10001","items":[{ "sku": "SKU", "qty": 2 }]}`

	return suite.Post("/reservations/hold", jsonStr)
}

func reserveItemBadRequestExpected(suite *reservationControllerTestSuite, res *httptest.ResponseRecorder, expectedMessage string) {
	suite.Equal(http.StatusBadRequest, res.Code)
	suite.Contains(res.Body.String(), "errors")
	suite.Contains(res.Body.String(), expectedMessage)
}

func (suite *reservationControllerTestSuite) Test_ReserveItems() {
	res := reserveItemTest(suite, nil)
	suite.Equal(http.StatusNoContent, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_AggregateError() {
	aggregateErr := commonErrors.AggregateError{}
	err := responses.InvalidSKUItemError{Sku: "SKU", Debug: "boom"}
	aggregateErr.Add(&err)

	res := reserveItemTest(suite, &aggregateErr)

	reserveItemBadRequestExpected(suite, res, `"sku":"SKU","afs":0,"debug":"boom"`)
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_OutOfStock() {
	res := reserveItemTest(suite, errors.New("boom"))

	reserveItemBadRequestExpected(suite, res, "boom")
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongSKUs() {
	res := reserveItemTest(suite, gorm.ErrRecordNotFound)

	suite.Equal(http.StatusNotFound, res.Code)
	suite.Contains(res.Body.String(), "errors")

	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_EmptySKUsList() {
	jsonStr := `{"refNum": "BR10001","items": []}`

	res := suite.Post("/reservations/hold", jsonStr)

	reserveItemBadRequestExpected(suite, res, "Reservation must have at least one SKU")
}

func (suite *reservationControllerTestSuite) Test_ReleaseItems() {
	suite.service.On("ReleaseItems", "BR10001").Return(nil).Once()

	res := suite.Delete("/reservations/hold/BR10001")

	suite.Equal(http.StatusNoContent, res.Code)
	suite.service.AssertExpectations(suite.T())
}

func (suite *reservationControllerTestSuite) Test_ReserveItems_WrongRefNum() {
	suite.service.On("ReleaseItems", "BR10001").Return(gorm.ErrRecordNotFound).Once()

	res := suite.Delete("/reservations/hold/BR10001")

	suite.Equal(http.StatusNotFound, res.Code)
	suite.Contains(res.Body.String(), "errors")

	suite.service.AssertExpectations(suite.T())
}
