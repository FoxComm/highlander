package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type shippingMethodControllerTestSuite struct {
	GeneralControllerTestSuite
	service *mocks.ShippingMethodServiceMock
}

func TestShippingMethodControllerSuite(t *testing.T) {
	suite.Run(t, new(shippingMethodControllerTestSuite))
}

func (suite *shippingMethodControllerTestSuite) SetupSuite() {
	suite.router = gin.New()

	suite.service = &mocks.ShippingMethodServiceMock{}

	controller := NewShippingMethodController(suite.service)
	controller.SetUp(suite.router.Group("/shipping-methods"))
}

func (suite *shippingMethodControllerTestSuite) TearDownTest() {
	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())

	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_EmptyData_ReturnsEmptyArray() {
	//arrange
	suite.service.On("GetShippingMethods").Return(&[]*models.ShippingMethod{}, nil).Once()

	//act
	shippingMethods := []*responses.ShippingMethod{}
	response := suite.Get("/shipping-methods", &shippingMethods)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(0, len(shippingMethods))
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_NonEmptyData_ReturnsRecordsArray() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	shippingMethod2 := fixtures.GetShippingMethod(uint(2), uint(2), fixtures.GetCarrier(uint(2)))
	suite.service.On("GetShippingMethods").Return([]*models.ShippingMethod{shippingMethod1, shippingMethod2}, nil).Once()

	//act
	shippingMethods := []*responses.ShippingMethod{}
	response := suite.Get("/shipping-methods", &shippingMethods)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(2, len(shippingMethods))
	suite.Equal(responses.NewShippingMethodFromModel(shippingMethod1), shippingMethods[0])
	suite.Equal(responses.NewShippingMethodFromModel(shippingMethod2), shippingMethods[1])
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.service.On("GetShippingMethodByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := &responses.Error{}
	response := suite.Get("/shipping-methods/1", errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_Found_ReturnsRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.service.On("GetShippingMethodByID", uint(1)).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod := &responses.ShippingMethod{}
	response := suite.Get("/shipping-methods/1", shippingMethod)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(responses.NewShippingMethodFromModel(shippingMethod1), shippingMethod)
}

func (suite *shippingMethodControllerTestSuite) Test_CreateShippingMethod_ReturnsRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	payload := fixtures.ToShippingMethodPayload(shippingMethod1)
	model, err := models.NewShippingMethodFromPayload(payload)
	suite.Nil(err)

	suite.service.On("CreateShippingMethod", model).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod := &responses.ShippingMethod{}
	response := suite.Post("/shipping-methods", payload, shippingMethod)

	//assert
	suite.Equal(http.StatusCreated, response.Code)
	suite.Equal(responses.NewShippingMethodFromModel(shippingMethod1), shippingMethod)
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.service.On("UpdateShippingMethod", fixtures.GetShippingMethod(uint(1), uint(1), &models.Carrier{})).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := &responses.Error{}
	response := suite.Put("/shipping-methods/1", fixtures.ToShippingMethodPayload(shippingMethod1), errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_Found_ReturnsRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.service.On("UpdateShippingMethod", fixtures.GetShippingMethod(uint(1), uint(1), &models.Carrier{})).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod := &responses.ShippingMethod{}
	response := suite.Put("/shipping-methods/1", fixtures.ToShippingMethodPayload(shippingMethod1), shippingMethod)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(responses.NewShippingMethodFromModel(shippingMethod1), shippingMethod)
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.service.On("DeleteShippingMethod", uint(1)).Return(gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Delete("/shipping-methods/1", &errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_Found() {
	//arrange
	suite.service.On("DeleteShippingMethod", uint(1)).Return(nil).Once()

	//act
	response := suite.Delete("/shipping-methods/1")

	//assert
	suite.Equal(http.StatusNoContent, response.Code)
	suite.Equal("", response.Body.String())
}
