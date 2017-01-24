package controllers

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/api/payloads"
	"github.com/FoxComm/highlander/middlewarehouse/api/responses"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/services"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type shippingMethodControllerTestSuite struct {
	GeneralControllerTestSuite
	db      *gorm.DB
	service services.ShippingMethodService
	carrier *models.Carrier
}

func TestShippingMethodControllerSuite(t *testing.T) {
	suite.Run(t, new(shippingMethodControllerTestSuite))
}

func (suite *shippingMethodControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.router = gin.New()

	suite.service = services.NewShippingMethodService(suite.db)

	controller := NewShippingMethodController(suite.service)
	controller.SetUp(suite.router.Group("/shipping-methods"))

	tasks.TruncateTables(suite.db, []string{
		"carriers",
	})

	suite.carrier = &models.Carrier{
		Name:             "Test",
		TrackingTemplate: "test.com/?=",
	}
	suite.Nil(suite.db.Create(suite.carrier).Error)
}

func (suite *shippingMethodControllerTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"shipping_methods",
	})
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_EmptyData_ReturnsEmptyArray() {
	//act
	shippingMethods := []*responses.ShippingMethod{}
	response := suite.Get("/shipping-methods", &shippingMethods)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(0, len(shippingMethods))
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethods_NonEmptyData_ReturnsRecordsArray() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(0), suite.carrier.ID, suite.carrier)
	shippingMethod1.Code = "METHOD1"
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	shippingMethod2 := fixtures.GetShippingMethod(uint(0), suite.carrier.ID, suite.carrier)
	shippingMethod2.Code = "METHOD2"
	suite.Nil(suite.db.Create(shippingMethod2).Error)

	//act
	shippingMethods := []*responses.ShippingMethod{}
	response := suite.Get("/shipping-methods", &shippingMethods)
	expectedResp1, err := responses.NewShippingMethodFromModel(shippingMethod1)
	suite.Nil(err)
	expectedResp2, err := responses.NewShippingMethodFromModel(shippingMethod2)
	suite.Nil(err)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(2, len(shippingMethods))
	suite.Equal(expectedResp1, shippingMethods[0])
	suite.Equal(expectedResp2, shippingMethods[1])
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//act
	errors := &responses.Error{}
	response := suite.Get("/shipping-methods/1", errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Sprintf(repositories.ErrorShippingMethodNotFound, 1), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_GetShippingMethodByID_Found_ReturnsRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(0), suite.carrier.ID, suite.carrier)
	shippingMethod1.Code = "METHOD1"
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	shippingMethod := &responses.ShippingMethod{}
	url := fmt.Sprintf("/shipping-methods/%d", shippingMethod1.ID)
	response := suite.Get(url, shippingMethod)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	expectedResp, err := responses.NewShippingMethodFromModel(shippingMethod1)
	suite.Nil(err)
	suite.Equal(expectedResp, shippingMethod)
}

func (suite *shippingMethodControllerTestSuite) Test_CreateShippingMethod_ReturnsRecord() {
	//arrange
	payload := &payloads.ShippingMethod{
		CarrierID:    suite.carrier.ID,
		Name:         "Pay the man",
		Code:         "PAYIT",
		ShippingType: "flat",
		Cost:         0,
	}

	//act
	shippingMethod := &responses.ShippingMethod{}
	response := suite.Post("/shipping-methods", payload, shippingMethod)

	//assert
	suite.Equal(http.StatusCreated, response.Code)
	suite.Equal(payload.Name, shippingMethod.Name)
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	payload := &payloads.ShippingMethod{
		CarrierID:    suite.carrier.ID,
		Name:         "Pay the man",
		Code:         "PAYIT",
		ShippingType: "flat",
		Cost:         0,
	}

	//act
	errors := &responses.Error{}
	response := suite.Put("/shipping-methods/29", payload, errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Sprintf(repositories.ErrorShippingMethodNotFound, 29), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_UpdateShippingMethod_Found_ReturnsRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(0), suite.carrier.ID, suite.carrier)
	shippingMethod1.Code = "METHOD1"
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	payload := &payloads.ShippingMethod{
		CarrierID:    suite.carrier.ID,
		Name:         "Pay the man",
		Code:         "PAYIT",
		ShippingType: "flat",
		Cost:         0,
	}

	//act
	shippingMethod := &responses.ShippingMethod{}
	url := fmt.Sprintf("/shipping-methods/%d", shippingMethod1.ID)
	response := suite.Put(url, payload, shippingMethod)

	//assert
	suite.Equal(http.StatusOK, response.Code)
	suite.Equal(payload.Name, shippingMethod.Name)
	suite.Equal(payload.Code, shippingMethod.Code)
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//act
	errors := responses.Error{}
	response := suite.Delete("/shipping-methods/1", &errors)

	//assert
	suite.Equal(http.StatusNotFound, response.Code)
	suite.Equal(1, len(errors.Errors))
	suite.Equal(fmt.Sprintf(repositories.ErrorShippingMethodNotFound, 1), errors.Errors[0])
}

func (suite *shippingMethodControllerTestSuite) Test_DeleteShippingMethod_Found() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(0), suite.carrier.ID, suite.carrier)
	shippingMethod1.Code = "METHOD1"
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	url := fmt.Sprintf("/shipping-methods/%d", shippingMethod1.ID)
	response := suite.Delete(url)

	//assert
	suite.Equal(http.StatusNoContent, response.Code)
	suite.Equal("", response.Body.String())
}
