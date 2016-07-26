package controllers

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/responses"
	"github.com/FoxComm/middlewarehouse/controllers/mocks"
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
	response, err := suite.Get("/carriers/", &[]responses.Carrier{})

	//assert
	suite.assert.Nil(err)
	result := response.(*[]responses.Carrier)
	suite.assert.Equal(0, len(*result))
}
