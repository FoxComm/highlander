package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

const carrierNotFoundError = "Carrier with id=%d not found"

type carrierControllerTestSuite struct {
	GeneralControllerTestSuite
	db      *gorm.DB
	service services.CarrierService
}

func TestCarrierControllerSuite(t *testing.T) {
	suite.Run(t, new(carrierControllerTestSuite))
}

func (suite *carrierControllerTestSuite) SetupSuite() {
	suite.router = gin.New()
	suite.db = config.TestConnection()

	suite.service = services.NewCarrierService(suite.db)

	controller := NewCarrierController(suite.service)
	controller.SetUp(suite.router.Group("/carriers"))
}

func (suite *carrierControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{"carriers"})
}

func (suite *carrierControllerTestSuite) Test_GetCarriers_EmptyData_ReturnsEmptyArray() {
	//act
	carriers := []*responses.Carrier{}
	response := suite.Get("/carriers", &carriers)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(0, len(carriers))
}

func (suite *carrierControllerTestSuite) Test_GetCarriers_NonEmptyData_ReturnsRecordsArray() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	carrier2 := fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(carrier1).Error)
	suite.Nil(suite.db.Create(carrier2).Error)

	//act
	carriers := []*responses.Carrier{}
	response := suite.Get("/carriers", &carriers)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(2, len(carriers))
	suite.Equal(responses.NewCarrierFromModel(carrier1), carriers[0])
	suite.Equal(responses.NewCarrierFromModel(carrier2), carriers[1])
}

func (suite *carrierControllerTestSuite) Test_GetCarrierByID_NotFound_ReturnsNotFoundError() {
	//act
	errors := &responses.Error{}
	response := suite.Get("/carriers/1", errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Errorf(carrierNotFoundError, 1).Error(), errors.Errors[0])
}

// func (suite *carrierControllerTestSuite) Test_GetCarrierByID_Found_ReturnsRecord() {
// 	//arrange
// 	carrier1 := fixtures.GetCarrier(uint(1))
// 	suite.service.On("GetCarrierByID", uint(1)).Return(carrier1, nil).Once()

// 	//act
// 	carrier := &responses.Carrier{}
// 	response := suite.Get("/carriers/1", carrier)

// 	//assert
// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(responses.NewCarrierFromModel(carrier1), carrier)
// }

// func (suite *carrierControllerTestSuite) Test_CreateCarrier_ReturnsRecord() {
// 	//arrange
// 	carrier1 := fixtures.GetCarrier(uint(1))
// 	payload := fixtures.ToCarrierPayload(carrier1)
// 	suite.service.On("CreateCarrier", payload.Model()).Return(carrier1, nil).Once()

// 	//act
// 	carrier := &responses.Carrier{}
// 	response := suite.Post("/carriers", fixtures.ToCarrierPayload(carrier1), carrier)

// 	//assert
// 	suite.Equal(http.StatusCreated, response.Code)
// 	suite.Equal(responses.NewCarrierFromModel(carrier1), carrier)
// }

// func (suite *carrierControllerTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
// 	//arrange
// 	carrier1 := fixtures.GetCarrier(uint(1))
// 	suite.service.On("UpdateCarrier", carrier1).Return(nil, gorm.ErrRecordNotFound).Once()

// 	//act
// 	errors := &responses.Error{}
// 	response := suite.Put("/carriers/1", fixtures.ToCarrierPayload(carrier1), errors)

// 	//assert
// 	suite.Equal(http.StatusNotFound, response.Code)
// 	suite.Equal(1, len(errors.Errors))
// 	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
// }

// func (suite *carrierControllerTestSuite) Test_UpdateCarrier_Found_ReturnsRecord() {
// 	//arrange
// 	carrier1 := fixtures.GetCarrier(uint(1))
// 	suite.service.On("UpdateCarrier", carrier1).Return(carrier1, nil).Once()

// 	//act
// 	carrier := &responses.Carrier{}
// 	response := suite.Put("/carriers/1", fixtures.ToCarrierPayload(carrier1), carrier)

// 	//assert
// 	suite.Equal(http.StatusOK, response.Code)
// 	suite.Equal(responses.NewCarrierFromModel(carrier1), carrier)
// }

// func (suite *carrierControllerTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
// 	//arrange
// 	suite.service.On("DeleteCarrier", uint(1)).Return(gorm.ErrRecordNotFound).Once()

// 	//act
// 	errors := &responses.Error{}
// 	response := suite.Delete("/carriers/1", errors)

// 	//assert
// 	suite.Equal(http.StatusNotFound, response.Code)
// 	suite.Equal(1, len(errors.Errors))
// 	suite.Equal(gorm.ErrRecordNotFound.Error(), errors.Errors[0])
// }

// func (suite *carrierControllerTestSuite) Test_DeleteCarrier_Found() {
// 	//arrange
// 	suite.service.On("DeleteCarrier", uint(1)).Return(nil).Once()

// 	//act
// 	response := suite.Delete("/carriers/1")

// 	//assert
// 	suite.Equal(http.StatusNoContent, response.Code)
// 	suite.Equal("", response.Body.String())
// }
