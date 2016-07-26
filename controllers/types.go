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

func (suite *GeneralControllerTestSuite) Get(url string, target interface{}) error {
	request, _ := http.NewRequest("GET", url, nil)

	return suite.getResponse(request, target)
}

func (suite *GeneralControllerTestSuite) getResponse(request *http.Request, target interface{}) error {
	//record response
	response := httptest.NewRecorder()

	//serve request with router, writing to response
	suite.router.ServeHTTP(response, request)

	//read body to []byte
	raw, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return err
	}

	//decode body to target object and return it
	decoder := json.NewDecoder(bytes.NewReader(raw))

	return decoder.Decode(target)
}
