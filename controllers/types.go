package controllers

import (
	"bytes"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"

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
	router *gin.Engine
}

func (suite *GeneralControllerTestSuite) Get(url string, target interface{}) (interface{}, error) {
	request, _ := http.NewRequest("GET", url, nil)
	response := httptest.NewRecorder()
	suite.router.ServeHTTP(response, request)

	return parseResponse(response, target)
}

func parseResponse(response *httptest.ResponseRecorder, target interface{}) (interface{}, error) {
	raw, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}

	decoder := json.NewDecoder(bytes.NewReader(raw))
	err = decoder.Decode(&target)

	return target, err
}
