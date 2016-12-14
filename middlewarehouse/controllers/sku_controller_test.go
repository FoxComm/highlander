package controllers

import (
	"encoding/json"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type skuControllerTestSuite struct {
	GeneralControllerTestSuite
	db  *gorm.DB
	sku *models.SKU
}

func TestSKUControllerTestSuite(t *testing.T) {
	suite.Run(t, new(skuControllerTestSuite))
}

func (suite *skuControllerTestSuite) SetupTest() {
	suite.db = config.TestConnection()
	tasks.TruncateTables(suite.db, []string{
		"skus",
	})

	suite.sku = fixtures.GetSKU()
	err := suite.db.Create(suite.sku).Error
	suite.Nil(err)

	suite.router = gin.Default()
	controller := NewSKUController(suite.db)
	controller.SetUp(suite.router.Group("/skus"))
}

func (suite *skuControllerTestSuite) Test_GetSKU_Success() {
	res := suite.Get("/skus/1")
	suite.Equal(http.StatusOK, res.Code)

	respBody := new(responses.SKU)
	err := json.NewDecoder(res.Body).Decode(respBody)
	suite.Nil(err)
	suite.Equal(suite.sku.ID, respBody.ID)
	suite.Equal(suite.sku.Code, respBody.Code)
}

func (suite *skuControllerTestSuite) Test_CreateSKU_Success() {
	payload := fixtures.GetCreateSKUPayload()
	res := suite.Post("/skus", payload)
	suite.Equal(http.StatusCreated, res.Code)

	respBody := new(responses.SKU)
	err := json.NewDecoder(res.Body).Decode(respBody)
	suite.Nil(err)
	suite.Equal(payload.Code, respBody.Code)
	suite.Equal(payload.UPC, respBody.UPC)
}
