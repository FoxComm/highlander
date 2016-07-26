package controllers

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierControllerTestSuite struct {
	GeneralControllerTestSuite
	service services.ICarrierService
}

func TestCarrierControllerSuite(t *testing.T) {
	suite.Run(t, new(CarrierControllerTestSuite))
}

func (suite *CarrierControllerTestSuite) SetupSuite() {
	suite.router = gin.Default()

	suite.service = mocks.NewCarrierServiceMock()

	controller := NewCarrierController(suite.service)
	controller.SetUp(suite.router.Group("/carriers"))

	suite.assert = assert.New(suite.T())
}

func (suite *CarrierControllerTestSuite) Test_GetCarriers_EmptyData() {
	//act
	result := []responses.Carrier{}
	err := suite.Get("/carriers/", &result)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(0, len(result))
}

func (suite *CarrierControllerTestSuite) Test_GetCarriers_NonEmptyData() {
	//arrange
	carrier1 := &models.Carrier{Name: "UPS", TrackingTemplate: "http://ups.com"}
	carrier2 := &models.Carrier{Name: "DHL", TrackingTemplate: "http://dhl.com"}
	suite.service.CreateCarrier(carrier1)
	suite.service.CreateCarrier(carrier2)

	//act
	result := []responses.Carrier{}
	err := suite.Get("/carriers/", &result)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(result))
	suite.assert.Equal(carrier1.Name, result[0].Name)
	suite.assert.Equal(carrier1.TrackingTemplate, result[0].TrackingTemplate)
	suite.assert.Equal(carrier2.Name, result[1].Name)
	suite.assert.Equal(carrier2.TrackingTemplate, result[1].TrackingTemplate)
}
