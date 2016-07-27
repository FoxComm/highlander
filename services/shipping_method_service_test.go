package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodServiceTestSuite struct {
	GeneralServiceTestSuite
	service IShippingMethodService
}

func TestShippingMethodServiceSuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodServiceTestSuite))
}

func (suite *ShippingMethodServiceTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.service = NewShippingMethodService(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodServiceTestSuite) TearDownTest() {
	// we make sure that all expectations were met
	assert.Nil(suite.T(), suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{
		CarrierID: uint(1),
		Name:      "UPS 2 day ground",
	}
	shippingMethod2 := &models.ShippingMethod{
		CarrierID: uint(2),
		Name:      "DHL 2 day ground",
	}
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(uint(1), shippingMethod1.CarrierID, shippingMethod1.Name).
		AddRow(uint(1), shippingMethod2.CarrierID, shippingMethod2.Name)
	suite.mock.ExpectQuery(`SELECT (.+) FROM "shipping_methods"`).WillReturnRows(rows)

	//act
	shippingMethods, err := suite.service.GetShippingMethods()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(shippingMethods))
	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethods[0].CarrierID)
	suite.assert.Equal(shippingMethod1.Name, shippingMethods[0].Name)
	suite.assert.Equal(shippingMethod2.CarrierID, shippingMethods[1].CarrierID)
	suite.assert.Equal(shippingMethod2.Name, shippingMethods[1].Name)
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodById_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{
		CarrierID: uint(1),
		Name:      "UPS 2 day ground",
	}
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(uint(1), shippingMethod1.CarrierID, shippingMethod1.Name)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "shipping_methods" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shippingMethod, err := suite.service.GetShippingMethodByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
}

func (suite *ShippingMethodServiceTestSuite) Test_CreaterShippingMethod_ReturnsIdOfCreatedRecord() {
	//arrange
	carrierID, name := uint(1), "UPS 2 day ground"
	model := &models.ShippingMethod{CarrierID: carrierID, Name: name}
	suite.mock.
		ExpectExec(`INSERT INTO "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	id, err := suite.service.CreateShippingMethod(model)

	//assert
	suite.assert.Equal(uint(1), id)
	suite.assert.Nil(err)
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_ReturnsNoError() {
	//arrange
	carrierID, name := uint(1), "UPS 2 day ground"
	suite.mock.
		ExpectExec(`UPDATE "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.service.UpdateShippingMethod(&models.ShippingMethod{ID: 1, CarrierID: carrierID, Name: name})

	//assert
	suite.assert.Nil(err)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "shipping_methods"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.service.DeleteShippingMethod(1)

	//assert
	suite.assert.Nil(err)
}
