package services

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodServiceTestSuite struct {
	GeneralServiceTestSuite
	service ShippingMethodService
	carrier *models.Carrier
}

func TestShippingMethodServiceSuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodServiceTestSuite))
}

func (suite *ShippingMethodServiceTestSuite) SetupSuite() {
	suite.db = config.TestConnection()
	suite.service = NewShippingMethodService(suite.db)
}

func (suite *ShippingMethodServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"carriers",
		"shipping_methods",
	})

	suite.carrier = fixtures.GetCarrier(uint(0))
	suite.Nil(suite.db.Create(suite.carrier).Error)
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), suite.carrier)
	shippingMethod2 := fixtures.GetShippingMethod(uint(2), uint(2), suite.carrier)
	shippingMethod2.Code = "STANDARD"
	suite.Nil(suite.db.Create(shippingMethod1).Error)
	suite.Nil(suite.db.Create(shippingMethod2).Error)

	//act
	shippingMethods, err := suite.service.GetShippingMethods()

	//assert
	suite.Nil(err)

	suite.Equal(2, len(shippingMethods))
	suite.Equal(shippingMethod1, shippingMethods[0])
	suite.Equal(shippingMethod2, shippingMethods[1])
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//act
	_, err := suite.service.GetShippingMethodByID(uint(1))

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1), err)
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodByID_Found_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), suite.carrier)
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	shippingMethod, err := suite.service.GetShippingMethodByID(shippingMethod1.ID)

	//assert
	suite.Nil(err)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_CreateShippingMethod_ReturnsCreatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), suite.carrier)
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	shippingMethod, err := suite.service.CreateShippingMethod(shippingMethod1)

	//assert
	suite.Nil(err)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))

	//act
	_, err := suite.service.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1), err)
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_Found_ReturnsUpdatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), suite.carrier)
	suite.Nil(suite.db.Create(shippingMethod1).Error)
	shippingMethod1.Cost = 5999

	//act
	shippingMethod, err := suite.service.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Nil(err)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.service.DeleteShippingMethod(uint(1))

	//assert
	suite.Equal(fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1), err)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_Found_ReturnsNoError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), suite.carrier)
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	err := suite.service.DeleteShippingMethod(uint(1))

	//assert
	suite.Nil(err)
}
