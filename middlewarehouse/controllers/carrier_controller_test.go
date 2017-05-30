package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type carrierControllerTestSuite struct {
	GeneralControllerTestSuite
	db      *gorm.DB
	service services.CarrierService
}

func TestCarrierControllerSuite(t *testing.T) {
	suite.Run(t, new(carrierControllerTestSuite))
}

func (suite *carrierControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	suite.service = services.NewCarrierService(suite.db)

	controller := NewCarrierController(suite.service)
	controller.SetUp(suite.router.Group("/carriers"))
}

func (suite *carrierControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{"carriers"})
}

func (suite *carrierControllerTestSuite) TearDownSuite() {
	suite.db.Close()
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
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1).Error(), errors.Errors[0])
}

func (suite *carrierControllerTestSuite) Test_GetCarrierByID_Found_ReturnsRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(carrier1).Error)
	url := fmt.Sprintf("/carriers/%d", carrier1.ID)

	//act
	carrier := &responses.Carrier{}
	response := suite.Get(url, carrier)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(responses.NewCarrierFromModel(carrier1), carrier)
}

func (suite *carrierControllerTestSuite) Test_CreateCarrier_ReturnsRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	payload := fixtures.ToCarrierPayload(carrier1)

	//act
	carrier := &responses.Carrier{}
	response := suite.Post("/carriers", payload, carrier)

	//assert
	suite.Equal(http.StatusCreated, response.Code)
	suite.Equal(payload.Name, carrier.Name)
	suite.Equal(payload.TrackingTemplate, carrier.TrackingTemplate)
	suite.Equal(payload.Scope, carrier.Scope)
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	payload := fixtures.ToCarrierPayload(carrier1)

	//act
	errors := &responses.Error{}
	response := suite.Put("/carriers/1", payload, errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1).Error(), errors.Errors[0])
}

func (suite *carrierControllerTestSuite) Test_UpdateCarrier_Found_ReturnsRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(carrier1).Error)
	payload := fixtures.ToCarrierPayload(carrier1)
	payload.Name = "Updated Carrier"
	url := fmt.Sprintf("/carriers/%d", carrier1.ID)

	//act
	carrier := &responses.Carrier{}
	response := suite.Put(url, payload, carrier)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(payload.Name, carrier.Name)
	suite.Equal(payload.TrackingTemplate, carrier.TrackingTemplate)
	suite.Equal(payload.Scope, carrier.Scope)
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//act
	errors := &responses.Error{}
	response := suite.Delete("/carriers/1", errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Errorf(repositories.ErrorCarrierNotFound, 1).Error(), errors.Errors[0])
}

func (suite *carrierControllerTestSuite) Test_DeleteCarrier_Found() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(carrier1).Error)
	url := fmt.Sprintf("/carriers/%d", carrier1.ID)

	//act
	response := suite.Delete(url)

	//assert
	suite.Equal(http.StatusNoContent, response.Code)
	suite.Equal("", response.Body.String())
}
