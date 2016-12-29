package repositories

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/common/tests"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/suite"
)

type ShippingMethodRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IShippingMethodRepository
	carrier1   *models.Carrier
}

func TestShippingMethodRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodRepositoryTestSuite))
}

func (suite *ShippingMethodRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewShippingMethodRepository(suite.db)

	tasks.TruncateTables(suite.db, []string{
		"carriers",
	})

	suite.carrier1 = fixtures.GetCarrier(1)
	suite.Nil(suite.db.Create(suite.carrier1).Error)
}

func (suite *ShippingMethodRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"shipping_methods",
	})
}

func (suite *ShippingMethodRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, suite.carrier1.ID, suite.carrier1)
	shippingMethod1.Code = "METHOD1"
	suite.Nil(suite.db.Create(shippingMethod1).Error)
	shippingMethod2 := fixtures.GetShippingMethod(2, suite.carrier1.ID, suite.carrier1)
	shippingMethod2.Code = "METHOD2"
	suite.Nil(suite.db.Create(shippingMethod2).Error)

	//act
	shippingMethods, err := suite.repository.GetShippingMethods()

	//assert
	suite.Nil(err)

	suite.Equal(2, len(shippingMethods))
	tests.SyncDates(shippingMethod1, shippingMethod2, shippingMethods[0], shippingMethods[1])
	suite.Equal(shippingMethod1, shippingMethods[0])
	suite.Equal(shippingMethod2, shippingMethods[1])
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//act
	_, err := suite.repository.GetShippingMethodByID(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShippingMethodNotFound, 1), err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_Found_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, suite.carrier1.ID, suite.carrier1)
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	shippingMethod, err := suite.repository.GetShippingMethodByID(shippingMethod1.ID)

	//assert
	suite.Nil(err)
	tests.SyncDates(shippingMethod1, shippingMethod)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_CreateShippingMethod_ReturnsCreatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, suite.carrier1.ID, suite.carrier1)

	//act
	shippingMethod, err := suite.repository.CreateShippingMethod(fixtures.GetShippingMethod(0, suite.carrier1.ID, suite.carrier1))

	//assert
	suite.Nil(err)
	tests.SyncDates(shippingMethod1, shippingMethod)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_CreateShippingMethodWithConditions_ReturnsCreatedRecord() {
	shippingMethod := fixtures.GetShippingMethod(0, suite.carrier1.ID, suite.carrier1)
	shippingMethod.Conditions = models.QueryStatement{
		Comparison: models.And,
		Conditions: []models.Condition{
			models.Condition{
				RootObject: "Order",
				Field:      "grandTotal",
				Operator:   models.Equals,
				Value:      100,
			},
		},
	}

	created, err := suite.repository.CreateShippingMethod(shippingMethod)

	suite.Nil(err)
	suite.Len(created.Conditions.Conditions, 1)
	suite.Equal("Order", created.Conditions.Conditions[0].RootObject)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, suite.carrier1.ID, suite.carrier1)

	//act
	_, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShippingMethodNotFound, shippingMethod1.ID), err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_Found_ReturnsUpdatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, 1, fixtures.GetCarrier(1))
	suite.Nil(suite.db.Create(shippingMethod1).Error)
	shippingMethod1.Name = "Other"

	//act
	shippingMethod, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Nil(err)
	tests.SyncDates(shippingMethod1, shippingMethod)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.repository.DeleteShippingMethod(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorShippingMethodNotFound, 1), err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_DeleteShippingMethod_Found_ReturnsNoError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(1, 1, fixtures.GetCarrier(1))
	suite.Nil(suite.db.Create(shippingMethod1).Error)

	//act
	err := suite.repository.DeleteShippingMethod(shippingMethod1.ID)

	//assert
	suite.Nil(err)
}
