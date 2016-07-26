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
	assert     *assert.Assertions
	controller IController
	server     *httptest.Server
}

func (suite *GeneralControllerTestSuite) Get(url string, target interface{}) (interface{}, error) {
	response, err := http.Get(suite.server.URL + url)
	if err != nil {
		return nil, err
	}

	return suite.parseResponse(response, target)
}

func (suite *GeneralControllerTestSuite) parseResponse(response *http.Response, target interface{}) (interface{}, error) {
	raw, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}

	decoder := json.NewDecoder(bytes.NewReader(raw))
	err = decoder.Decode(&target)

	return target, err
}
