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
	"github.com/FoxComm/highlander/middlewarehouse/models/rules"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/services"
	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/suite"
)

type orderShippingMethodControllerTestSuite struct {
	GeneralControllerTestSuite
	db      *gorm.DB
	carrier *models.Carrier
}

func TestOrderShippingMethodControllerSuite(t *testing.T) {
	suite.Run(t, new(orderShippingMethodControllerTestSuite))
}

func (suite *orderShippingMethodControllerTestSuite) SetupTest() {
	suite.db = config.TestConnection()
	tasks.TruncateTables(suite.db, []string{
		"shipping_methods",
		"carriers",
	})

	suite.carrier = fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(suite.carrier).Error)

	suite.router = gin.Default()

	repo := repositories.NewShippingMethodRepository(suite.db)
	service := services.NewShippingMethodService(repo)
	controller := NewOrderShippingMethodController(service)
	controller.SetUp(suite.router.Group("/order-shipping-methods"))
}

func (suite *orderShippingMethodControllerTestSuite) Test_GetShippingMethods_Success() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 1)
}

func (suite *orderShippingMethodControllerTestSuite) Test_EvaluateMethod_GrandTotalSuccess() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	shippingMethod.Conditions = fixtures.GrandTotalRole(rules.GreaterThan, 25)
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 1)
}

func (suite *orderShippingMethodControllerTestSuite) Test_EvaluateMethod_GrandTotalFailure() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	shippingMethod.Conditions = fixtures.GrandTotalRole(rules.GreaterThan, 100)
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	payload.Totals.Total = 90
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 0)
}

func (suite *orderShippingMethodControllerTestSuite) Test_EvaluateMethod_ShippingRegionSuccess() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	shippingMethod.Conditions = fixtures.WestCoastRole()
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	payload.ShippingAddress = fixtures.GetCaliforniaAddressPayload()
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 1)
}

func (suite *orderShippingMethodControllerTestSuite) Test_EvaluateMethod_ShippingRegionAndAmountSuccess() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	shippingMethod.Conditions = fixtures.WestCoastAndTotalRole()
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	payload.Totals.Total = 50
	payload.ShippingAddress = fixtures.GetCaliforniaAddressPayload()
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 1)
}

func (suite *orderShippingMethodControllerTestSuite) Test_EvaluateMethod_ShippingRegionAndAmountFailure() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier.ID, suite.carrier)
	shippingMethod.Conditions = fixtures.WestCoastAndTotalRole()
	suite.Nil(suite.db.Create(shippingMethod).Error)

	payload := fixtures.GetOrder()
	payload.Totals.Total = 500
	payload.ShippingAddress = fixtures.GetCaliforniaAddressPayload()
	res := suite.Post("/order-shipping-methods", payload)

	suite.Equal(http.StatusOK, res.Code)

	methods := []*responses.OrderShippingMethod{}
	err := json.NewDecoder(res.Body).Decode(&methods)

	suite.Nil(err)
	suite.Len(methods, 0)
}
