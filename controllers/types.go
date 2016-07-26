package controllers

import (
	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type IController interface {
	SetUp(router gin.IRouter)
}

type GeneralControllerTestSuite struct {
	suite.Suite
	assert *assert.Assertions
}
