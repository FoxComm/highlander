package controllers

// import (
// 	"encoding/json"
// 	"fmt"
// 	"net/http"
// 	"testing"

// 	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
// 	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
// 	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
// 	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
// 	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
// 	"github.com/FoxComm/highlander/middlewarehouse/models"
// 	"github.com/gin-gonic/gin"
// 	"github.com/jinzhu/gorm"
// 	"github.com/stretchr/testify/suite"
// )

// type skuControllerTestSuite struct {
// 	GeneralControllerTestSuite
// 	db  *gorm.DB
// 	sku *models.SKU
// }

// func TestSKUControllerTestSuite(t *testing.T) {
// 	suite.Run(t, new(skuControllerTestSuite))
// }

// func (suite *skuControllerTestSuite) SetupTest() {
// 	suite.db = config.TestConnection()
// 	tasks.TruncateTables(suite.db, []string{
// 		"skus",
// 	})

// 	suite.sku = fixtures.GetSKU()
// 	err := suite.db.Create(suite.sku).Error
// 	suite.Nil(err)

// 	suite.router = gin.Default()
// 	controller := NewSKUController(suite.db)
// 	controller.SetUp(suite.router.Group("/skus"))
// }

// func (suite *skuControllerTestSuite) Test_GetSKU_Success() {
// 	res := suite.Get("/skus/1")
// 	suite.Equal(http.StatusOK, res.Code)

// 	respBody := new(responses.SKU)
// 	err := json.NewDecoder(res.Body).Decode(respBody)
// 	suite.Nil(err)
// 	suite.Equal(suite.sku.ID, respBody.ID)
// 	suite.Equal(suite.sku.Code, respBody.Code)
// }

// func (suite *skuControllerTestSuite) Test_CreateSKU_Success() {
// 	payload := fixtures.GetCreateSKUPayload()
// 	res := suite.Post("/skus", payload)
// 	suite.Equal(http.StatusCreated, res.Code)

// 	respBody := new(responses.SKU)
// 	err := json.NewDecoder(res.Body).Decode(respBody)
// 	suite.Nil(err)
// 	suite.Equal(payload.Code, respBody.Code)
// 	suite.Equal(payload.UPC, respBody.UPC)
// }

// func (suite *skuControllerTestSuite) Test_UpdateSKUCode_Success() {
// 	code := "UPDATED"
// 	payload := &payloads.UpdateSKU{Code: &code}

// 	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
// 	res := suite.Patch(url, payload)
// 	suite.Equal(http.StatusOK, res.Code)
// }

// func (suite *skuControllerTestSuite) Test_UpdateSKUCodeAndRequest_Success() {
// 	code := "UPDATED"
// 	payload := &payloads.UpdateSKU{Code: &code}

// 	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
// 	res := suite.Patch(url, payload)
// 	suite.Equal(http.StatusOK, res.Code)

// 	getRes := suite.Get(url)
// 	suite.Equal(http.StatusOK, getRes.Code)

// 	respBody := new(responses.SKU)
// 	err := json.NewDecoder(getRes.Body).Decode(respBody)
// 	suite.Nil(err)
// 	suite.Equal(suite.sku.ID, respBody.ID)
// 	suite.Equal(code, respBody.Code)
// }

// func (suite *skuControllerTestSuite) Test_UpdateSKUCodeBlank_Failure() {
// 	code := ""
// 	payload := &payloads.UpdateSKU{Code: &code}

// 	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
// 	res := suite.Patch(url, payload)
// 	suite.Equal(http.StatusBadRequest, res.Code)
// }

// func (suite *skuControllerTestSuite) Test_DeleteSKU_Success() {
// 	url := fmt.Sprintf("/skus/%d", suite.sku.ID)
// 	res := suite.Delete(url)
// 	suite.Equal(http.StatusNoContent, res.Code)
// }
