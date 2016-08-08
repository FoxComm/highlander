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

func (suite *carrierControllerTestSuite) Test_GetCarriers_EmptyData_ReturnsEmptyArray() {
	//arrange
	suite.service.On("GetCarriers").Return(&[]*models.Carrier{}, nil).Once()

	//act
	carriers := []responses.Carrier{}
	response := suite.Get("/carriers", &carriers)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(0, len(carriers))

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarriers_NonEmptyData_ReturnsRecordsArray() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "http://ups.com"}
	carrier2 := &models.Carrier{uint(2), "DHL", "http://dhl.com"}
	suite.service.On("GetCarriers").Return([]*models.Carrier{carrier1, carrier2}, nil).Once()

	//act
	carriers := []responses.Carrier{}
	response := suite.Get("/carriers", &carriers)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1.ID, carriers[0].ID)
	suite.assert.Equal(carrier1.Name, carriers[0].Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carriers[0].TrackingTemplate)
	suite.assert.Equal(carrier2.ID, carriers[1].ID)
	suite.assert.Equal(carrier2.Name, carriers[1].Name)
	suite.assert.Equal(carrier2.TrackingTemplate, carriers[1].TrackingTemplate)

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarrierByID_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.service.On("GetCarrierByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Get("/carriers/1", &errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_GetCarrierByID_Found_ReturnsRecord() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "http://ups.com"}
	suite.service.On("GetCarrierByID", uint(1)).Return(carrier1, nil).Once()

	//act
	carrier := responses.Carrier{}
	response := suite.Get("/carriers/1", &carrier)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(carrier1.ID, carrier.ID)
	suite.assert.Equal(carrier1.Name, carrier.Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carrier.TrackingTemplate)

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_CreateCarrier_ReturnsRecord() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "http://ups.com"}
	carrier1Model := models.NewCarrierFromPayload(carrier1)
	suite.service.On("CreateCarrier", carrier1Model).Return(carrier1Model, nil).Once()

	//act
	carrier := responses.Carrier{}
	response := suite.Post("/carriers", carrier1, &carrier)

	//assert
	suite.assert.Equal(http.StatusCreated, response.Code)
	suite.assert.Equal(carrier1.Name, carrier.Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carrier.TrackingTemplate)

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "http://ups.com"}
	carrier1Model := models.NewCarrierFromPayload(carrier1)
	carrier1Model.ID = 1
	suite.service.On("UpdateCarrier", carrier1Model).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Put("/carriers/1", carrier1, &errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_Found_ReturnsRecord() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "http://ups.com"}
	carrier1Model := models.NewCarrierFromPayload(carrier1)
	carrier1Model.ID = 1
	suite.service.On("UpdateCarrier", carrier1Model).Return(carrier1Model, nil).Once()

	//act
	carrier := responses.Carrier{}
	response := suite.Put("/carriers/1", carrier1, &carrier)

	//assert
	suite.assert.Equal(http.StatusOK, response.Code)
	suite.assert.Equal(carrier1.Name, carrier.Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carrier.TrackingTemplate)

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.service.On("DeleteCarrier", uint(1)).Return(gorm.ErrRecordNotFound).Once()

	//act
	errors := responses.Error{}
	response := suite.Delete("/carriers/1", &errors)

	//assert
	suite.assert.Equal(http.StatusNotFound, response.Code)
	suite.assert.Equal(1, len(errors.Errors))
	suite.assert.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_Found() {
	//arrange
	suite.service.On("DeleteCarrier", uint(1)).Return(nil).Once()

	//act
	response := suite.Delete("/carriers/1")

	//assert
	suite.assert.Equal(http.StatusNoContent, response.Code)
	suite.assert.Equal("", response.Body.String())

	//assert all expectations were met
	suite.service.AssertExpectations(suite.T())
}
