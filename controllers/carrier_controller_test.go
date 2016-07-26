package controllers

import (
	"net/http"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type carrierControllerTestSuite struct {
	GeneralControllerTestSuite
	service *mocks.CarrierServiceMock
}

func TestCarrierControllerSuite(t *testing.T) {
	suite.Run(t, new(carrierControllerTestSuite))
}

func (suite *carrierControllerTestSuite) SetupSuite() {
	suite.router = gin.New()

	suite.service = &mocks.CarrierServiceMock{}

	controller := NewCarrierController(suite.service)
	controller.SetUp(suite.router.Group("/carriers"))

	suite.assert = assert.New(suite.T())
}

func (suite *carrierControllerTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.service.ExpectedCalls = []*mock.Call{}
	suite.service.Calls = []mock.Call{}
}

func (suite *carrierControllerTestSuite) Test_GetCarriers_EmptyData() {
	//arrange
	suite.service.On("GetCarriers").Return(&[]*models.Carrier{}, nil).Once()

	//act
	result := []responses.Carrier{}
	code, err := suite.Get("/carriers/", &result)

	//assert
	suite.assert.Equal(http.StatusOK, code)
	suite.assert.Nil(err)
	suite.assert.Equal(0, len(result))
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarriers_NonEmptyData() {
	//arrange
	carrier1 := &models.Carrier{Name: "UPS", TrackingTemplate: "http://ups.com"}
	carrier2 := &models.Carrier{Name: "DHL", TrackingTemplate: "http://dhl.com"}
	suite.service.On("GetCarriers").Return([]*models.Carrier{
		carrier1,
		carrier2,
	}, nil).Once()

	//act
	result := []responses.Carrier{}
	code, err := suite.Get("/carriers/", &result)

	//assert
	suite.assert.Equal(http.StatusOK, code)
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(result))
	suite.assert.Equal(carrier1.Name, result[0].Name)
	suite.assert.Equal(carrier1.TrackingTemplate, result[0].TrackingTemplate)
	suite.assert.Equal(carrier2.Name, result[1].Name)
	suite.assert.Equal(carrier2.TrackingTemplate, result[1].TrackingTemplate)
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarrierByID_NotFound() {
	//arrange
	suite.service.On("GetCarrierByID").Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	code, err := suite.Get("/carriers/1", &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, code)
	suite.assert.Nil(err)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarrierById_Found() {
	//arrange
	carrier := &models.Carrier{Name: "UPS", TrackingTemplate: "http://ups.com"}
	suite.service.On("GetCarrierByID").Return(carrier, nil).Once()

	//act
	result := responses.Carrier{}
	code, err := suite.Get("/carriers/1", &result)

	//assert
	suite.assert.Equal(http.StatusOK, code)
	suite.assert.Nil(err)
	suite.assert.Equal(carrier.Name, result.Name)
	suite.assert.Equal(carrier.TrackingTemplate, result.TrackingTemplate)
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_CreateCarrier() {
	//arrange
	carrier := &models.Carrier{Name: "UPS", TrackingTemplate: "http://ups.com"}
	suite.service.On("CreateCarrier").Return(uint(1), nil).Once()

	//act
	var result uint
	code, err := suite.Post("/carriers/", carrier, &result)

	//assert
	suite.assert.Equal(http.StatusCreated, code)
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), result)
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_NotFound() {
	//arrange
	updated := &models.Carrier{Name: "DHL", TrackingTemplate: "http://dhl.com"}
	suite.service.On("UpdateCarrier").Return(false, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	code, err := suite.Put("/carriers/1", updated, &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, code)
	suite.assert.Nil(err)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_Found() {
	//arrange
	carrier := &models.Carrier{Name: "UPS", TrackingTemplate: "http://ups.com"}
	suite.service.On("UpdateCarrier").Return(true).Once()

	//act
	var result string
	code, err := suite.Put("/carriers/1", carrier, &result)

	//assert
	suite.assert.Equal(http.StatusNoContent, code)
	suite.assert.Nil(err)
	suite.assert.Equal("", result)
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_NotFound() {
	//arrange
	suite.service.On("DeleteCarrier").Return(false, gorm.ErrRecordNotFound).Once()

	//act
	result := responses.Error{}
	code, err := suite.Delete("/carriers/1", &result)

	//assert
	suite.assert.Equal(http.StatusNotFound, code)
	suite.assert.Nil(err)
	suite.assert.Equal(1, len(result.Errors))
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_Found() {
	//arrange
	suite.service.On("DeleteCarrier").Return(true).Once()

	//act
	var result string
	code, err := suite.Delete("/carriers/1", &result)

	//assert
	suite.assert.Equal(http.StatusNoContent, code)
	suite.assert.Nil(err)
	suite.assert.Equal("", result)
	suite.service.AssertExpectations(suite.T())
}
