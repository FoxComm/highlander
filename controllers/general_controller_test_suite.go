package controllers

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type GeneralControllerTestSuite struct {
	suite.Suite
	assert *assert.Assertions
	router *gin.Engine
}

func (suite *GeneralControllerTestSuite) Get(url string, target interface{}) (int, error) {
	request, _ := http.NewRequest("GET", url, nil)

	return suite.getResponse(request, target)
}

func (suite *GeneralControllerTestSuite) Post(url string, body interface{}, target interface{}) (int, error) {
	request, _ := http.NewRequest("POST", url, prepareBody(body))

	return suite.getResponse(request, target)
}

func (suite *GeneralControllerTestSuite) Put(url string, body interface{}, target interface{}) (int, error) {
	request, _ := http.NewRequest("PUT", url, prepareBody(body))

	return suite.getResponse(request, target)
}

func (suite *GeneralControllerTestSuite) Delete(url string, target interface{}) (int, error) {
	request, _ := http.NewRequest("DELETE", url, nil)

	return suite.getResponse(request, target)
}

func (suite *GeneralControllerTestSuite) getResponse(request *http.Request, target interface{}) (int, error) {
	//record response
	response := httptest.NewRecorder()

	//serve request with router, writing to response
	suite.router.ServeHTTP(response, request)

	return response.Code, parseBody(response.Body, target)
}

func prepareBody(raw interface{}) io.Reader {
	buffer := new(bytes.Buffer)
	json.NewEncoder(buffer).Encode(raw)

	return buffer
}

func parseBody(body *bytes.Buffer, target interface{}) error {
	//nothing to read, if empty buffer
	if body.Len() == 0 {
		return nil
	}

	//decode body to target object and return it
	decoder := json.NewDecoder(body)

	return decoder.Decode(target)
}
