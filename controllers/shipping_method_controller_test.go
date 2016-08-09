package controllers
//
//import (
//	"net/http"
//	"testing"
//
//	"github.com/FoxComm/middlewarehouse/api/payloads"
//	"github.com/FoxComm/middlewarehouse/api/responses"
//	"github.com/FoxComm/middlewarehouse/controllers/mocks"
//	"github.com/FoxComm/middlewarehouse/models"
//
//	"github.com/gin-gonic/gin"
//	"github.com/jinzhu/gorm"
//	"github.com/stretchr/testify/assert"
//	"github.com/stretchr/testify/mock"
//	"github.com/stretchr/testify/suite"
//)
//
//type shippingMethodControllerTestSuite struct {
//	GeneralControllerTestSuite
//	service *mocks.ShippingMethodServiceMock
//}
//
//func TestShippingMethodControllerSuite(t *testing.T) {
//	suite.Run(t, new(shippingMethodControllerTestSuite))
//}
//
//func (suite *shippingMethodControllerTestSuite) SetupSuite() {
//	suite.router = gin.New()
//
//	suite.service = &mocks.ShippingMethodServiceMock{}
//
//	controller := NewShippingMethodController(suite.service)
//	controller.SetUp(suite.router.Group("/shipping-methods"))
//
//	suite.assert = assert.New(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) TearDownTest() {
//	// clear service mock calls expectations after each test
//	suite.service.ExpectedCalls = []*mock.Call{}
//	suite.service.Calls = []mock.Call{}
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_EmptyData_ReturnsEmptyArray() {
//	//arrange
//	suite.service.On("GetShippingMethods").Return(&[]*models.ShippingMethod{}, nil).Once()
//
//	//act
//	shippingMethods := []responses.ShippingMethod{}
//	response := suite.Get("/shipping-methods", &shippingMethods)
//
//	//assert
//	suite.assert.Equal(http.StatusOK, response.Code)
//	suite.assert.Equal(0, len(shippingMethods))
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_NonEmptyData_ReturnsRecordsArray() {
//	//arrange
//	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 day ground"}
//	shippingMethod2 := &models.ShippingMethod{uint(2), uint(2), "DHL 2 day ground"}
//	suite.service.On("GetShippingMethods").Return([]*models.ShippingMethod{shippingMethod1, shippingMethod2}, nil).Once()
//
//	//act
//	shippingMethods := []responses.ShippingMethod{}
//	response := suite.Get("/shipping-methods", &shippingMethods)
//
//	//assert
//	suite.assert.Equal(http.StatusOK, response.Code)
//	suite.assert.Equal(2, len(shippingMethods))
//	suite.assert.Equal(shippingMethod1.ID, shippingMethods[0].ID)
//	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethods[0].CarrierID)
//	suite.assert.Equal(shippingMethod1.Name, shippingMethods[0].Name)
//	suite.assert.Equal(shippingMethod2.ID, shippingMethods[1].ID)
//	suite.assert.Equal(shippingMethod2.CarrierID, shippingMethods[1].CarrierID)
//	suite.assert.Equal(shippingMethod2.Name, shippingMethods[1].Name)
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
//	//arrange
//	suite.service.On("GetShippingMethodByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()
//
//	//act
//	errors := responses.Error{}
//	response := suite.Get("/shipping-methods/1", &errors)
//
//	//assert
//	suite.assert.Equal(http.StatusNotFound, response.Code)
//	suite.assert.Equal(1, len(errors.Errors))
//	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_Found_ReturnsRecord() {
//	//arrange
//	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 day ground"}
//	suite.service.On("GetShippingMethodByID", uint(1)).Return(shippingMethod1, nil).Once()
//
//	//act
//	shippingMethod := responses.ShippingMethod{}
//	response := suite.Get("/shipping-methods/1", &shippingMethod)
//
//	//assert
//	suite.assert.Equal(http.StatusOK, response.Code)
//	suite.assert.Equal(shippingMethod1.ID, shippingMethod.ID)
//	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethod.CarrierID)
//	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_CreateShippingMethod_ReturnsRecord() {
//	//arrange
//	shippingMethod1 := &payloads.ShippingMethod{uint(1), "UPS 2 day ground"}
//	shippingMethod1Model := models.NewShippingMethodFromPayload(shippingMethod1)
//	suite.service.On("CreateShippingMethod", shippingMethod1Model).Return(shippingMethod1Model, nil).Once()
//
//	//act
//	shippingMethod := responses.ShippingMethod{}
//	response := suite.Post("/shipping-methods", shippingMethod1, &shippingMethod)
//
//	//assert
//	suite.assert.Equal(http.StatusCreated, response.Code)
//	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethod.CarrierID)
//	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
//	//arrange
//	shippingMethod1 := &payloads.ShippingMethod{uint(1), "UPS 2 day ground"}
//	shippingMethod1Model := models.NewShippingMethodFromPayload(shippingMethod1)
//	shippingMethod1Model.ID = 1
//	suite.service.On("UpdateShippingMethod", shippingMethod1Model).Return(nil, gorm.ErrRecordNotFound).Once()
//
//	//act
//	errors := responses.Error{}
//	response := suite.Put("/shipping-methods/1", shippingMethod1, &errors)
//
//	//assert
//	suite.assert.Equal(http.StatusNotFound, response.Code)
//	suite.assert.Equal(1, len(errors.Errors))
//	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_Found_ReturnsRecord() {
//	//arrange
//	shippingMethod1 := &payloads.ShippingMethod{uint(1), "UPS 2 day ground"}
//	shippingMethod1Model := models.NewShippingMethodFromPayload(shippingMethod1)
//	shippingMethod1Model.ID = 1
//	suite.service.On("UpdateShippingMethod", shippingMethod1Model).Return(shippingMethod1Model, nil).Once()
//
//	//act
//	shippingMethod := responses.ShippingMethod{}
//	response := suite.Put("/shipping-methods/1", shippingMethod1, &shippingMethod)
//
//	//assert
//	suite.assert.Equal(http.StatusOK, response.Code)
//	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethod.CarrierID)
//	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
//	//arrange
//	suite.service.On("DeleteShippingMethod", uint(1)).Return(gorm.ErrRecordNotFound).Once()
//
//	//act
//	errors := responses.Error{}
//	response := suite.Delete("/shipping-methods/1", &errors)
//
//	//assert
//	suite.assert.Equal(http.StatusNotFound, response.Code)
//	suite.assert.Equal(1, len(errors.Errors))
//	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
//
//func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_Found() {
//	//arrange
//	suite.service.On("DeleteShippingMethod", uint(1)).Return(nil).Once()
//
//	//act
//	response := suite.Delete("/shipping-methods/1")
//
//	//assert
//	suite.assert.Equal(http.StatusNoContent, response.Code)
//	suite.assert.Equal("", response.Body.String())
//
//	//assert all expectations were met
//	suite.service.AssertExpectations(suite.T())
//}
