package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
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
	controller.SetUp(suite.router.Group("/shippingMethods"))

	suite.assert = assert.New(suite.T())
}

func (suite *shippingMethodControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_EmptyData() {
	//arrange
	suite.service.On("GetShippingMethods").Return(&[]*models.ShippingMethod{}, nil).Once()

	//act
	result := []responses.ShippingMethod{}
	response := suite.Get("/shippingMethods/", &result)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(0, len(result))
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_NonEmptyData() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{CarrierID: uint(1), Name: "UPS 2 days ground"}
	shippingMethod2 := &models.ShippingMethod{CarrierID: uint(2), Name: "DHL 2 days ground"}
	suite.service.On("GetShippingMethods").Return([]*models.ShippingMethod{
		shippingMethod1,
		shippingMethod2,
	}, nil).Once()

	//act
	result := []responses.ShippingMethod{}
	response := suite.Get("/shippingMethods/", &result)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(2, len(result))
	suite.assert.Equal(shippingMethod1.CarrierID, result[0].CarrierID)
	suite.assert.Equal(shippingMethod1.Name, result[0].Name)
	suite.assert.Equal(shippingMethod2.CarrierID, result[1].CarrierID)
	suite.assert.Equal(shippingMethod2.Name, result[1].Name)
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_NotFound() {
	//arrange
	suite.service.On("GetShippingMethodByID").Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	response := suite.Get("/shippingMethods/1", &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_Found() {
	//arrange
	shippingMethod := &models.ShippingMethod{CarrierID: uint(1), Name: "UPS 2 days ground"}
	suite.service.On("GetShippingMethodByID").Return(shippingMethod, nil).Once()

	//act
	result := responses.ShippingMethod{}
	response := suite.Get("/shippingMethods/1", &result)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(shippingMethod.CarrierID, result.CarrierID)
	suite.assert.Equal(shippingMethod.Name, result.Name)
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_CreateShippingMethod() {
	//arrange
	shippingMethod := &payloads.ShippingMethod{CarrierID: uint(1), Name: "UPS 2 days ground"}
	suite.service.On("CreateShippingMethod").Return(uint(1), nil).Once()

	//act
	var result uint
	response := suite.Post("/shippingMethods/", shippingMethod, &result)

	//assert
	suite.assert.Equal(http.StatusCreated, response.Code)
	suite.assert.Equal(uint(1), result)
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_NotFound() {
	//arrange
	shippingMethod := &payloads.ShippingMethod{CarrierID: uint(1), Name: "UPS 2 days ground"}
	suite.service.On("UpdateShippingMethod").Return(false, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	response := suite.Put("/shippingMethods/1", shippingMethod, &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_Found() {
	//arrange
	shippingMethod := &payloads.ShippingMethod{CarrierID: uint(1), Name: "UPS 2 days ground"}
	suite.service.On("UpdateShippingMethod").Return(true).Once()

	//act
	var result string
	response := suite.Put("/shippingMethods/1", shippingMethod, &result)

	//assert
	suite.assert.Equal(http.StatusNoContent, response.Code)
	suite.assert.Equal("", result)
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_NotFound() {
	//arrange
	suite.service.On("DeleteShippingMethod").Return(false, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	response := suite.Delete("/shippingMethods/1", &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_Found() {
	//arrange
	suite.service.On("DeleteShippingMethod").Return(true).Once()

	//act
	response := suite.Delete("/shippingMethods/1")

	//assert
	suite.assert.Equal(http.StatusNoContent, response.Code)
	suite.assert.Equal("", response.Body.String())
	suite.service.AssertExpectations(suite.T())
}
