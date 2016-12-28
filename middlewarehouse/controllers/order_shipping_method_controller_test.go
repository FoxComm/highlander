package controllers

import (
	"net/http"
	"testing"

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

type orderShippingMethodControllerTestSuite struct {
	GeneralControllerTestSuite
	db             *gorm.DB
	shippingMethod *models.ShippingMethod
}

func TestOrderShippingMethodControllerSuite(t *testing.T) {
	suite.Run(t, new(orderShippingMethodControllerTestSuite))
}

func (suite *orderShippingMethodControllerTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	tasks.TruncateTables(suite.db, []string{
		"shipping_methods",
		"carriers",
	})

	carrier := fixtures.GetCarrier(1)
	err := suite.db.Create(carrier).Error
	suite.Nil(err)

	suite.shippingMethod = fixtures.GetShippingMethod(1, carrier.ID, carrier)

	err = suite.db.Create(suite.shippingMethod).Error
	suite.Nil(err)

	suite.router = gin.Default()

	repo := repositories.NewShippingMethodRepository(suite.db)
	service := services.NewShippingMethodService(repo)
	controller := NewOrderShippingMethodController(service)
	controller.SetUp(suite.router.Group("/order-shipping-methods"))
}

func (suite *orderShippingMethodControllerTestSuite) Test_GetShippingMethods_Success() {
	payload := fixtures.GetOrder()
	res := suite.Post("/order-shipping-methods", payload)
	suite.Equal(http.StatusOK, res.Code)
}
