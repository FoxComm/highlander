package controllers

import (
	"net/http/httptest"
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

func (suite *CarrierControllerTestSuite) SetupTest() {
	engine := gin.Default()

	suite.service = mocks.NewCarrierServiceMock()

	suite.controller = NewCarrierController(suite.service)
	suite.controller.SetUp(engine.Group("/carriers"))

	suite.server = httptest.NewServer(engine)

	suite.assert = assert.New(suite.T())
}

func (suite *CarrierControllerTestSuite) TearDownTest() {
	suite.server.Close()
}

func (suite *CarrierControllerTestSuite) Test_GetCarriers_EmptyData() {
	//act
	response, err := suite.Get("/carriers", &[]responses.Carrier{})

	//assert
	suite.assert.Nil(err)
	result := response.(*[]responses.Carrier)
	suite.assert.Equal(0, len(*result))
}
