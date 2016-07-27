package controllers

import (
	"net/http"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type pingControllerTestSuite struct {
	GeneralControllerTestSuite
}

func TestPingControllerSuite(t *testing.T) {
	suite.Run(t, new(pingControllerTestSuite))
}

func (suite *pingControllerTestSuite) SetupSuite() {
	suite.assert = assert.New(suite.T())
	// set up test env once
	suite.router = gin.Default()

	controller := NewPingController()
	controller.SetUp(suite.router.Group("/ping"))
}

func (suite *pingControllerTestSuite) Test_Ping() {
	res := suite.Get("/ping/")

	suite.assert.Equal(http.StatusOK, res.Code)
	suite.assert.Equal("{}\n", res.Body.String())
}
