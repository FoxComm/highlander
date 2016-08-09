package repositories

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IShippingMethodRepository
}

func TestShippingMethodRepositorySuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodRepositoryTestSuite))
}

func (suite *ShippingMethodRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.repository = NewShippingMethodRepository(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodRepositoryTestSuite) TearDownTest() {
	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	carrier2 := fixtures.GetCarrier(uint(2))
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), carrier1)
	shippingMethod2 := fixtures.GetShippingMethod(uint(2), uint(2), carrier2)
	shippingMethodRows := sqlmock.
		NewRows(fixtures.GetShippingMethodColumns()).
		AddRow(fixtures.GetShippingMethodRow(shippingMethod1)...).
		AddRow(fixtures.GetShippingMethodRow(shippingMethod2)...)
	suite.mock.ExpectQuery(`SELECT .+ FROM "shipping_methods"`).WillReturnRows(shippingMethodRows)
	carrierRows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(carrier1)...).
		AddRow(fixtures.GetCarrierRow(carrier2)...)
	suite.mock.ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" IN \(\?,\?\)\)`).
		WithArgs(carrier1.ID, carrier2.ID).
		WillReturnRows(carrierRows)

	//act
	shippingMethods, err := suite.repository.GetShippingMethods()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(shippingMethods))
	suite.assert.Equal(shippingMethod1, shippingMethods[0])
	suite.assert.Equal(shippingMethod2, shippingMethods[1])
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(fixtures.GetShippingMethodColumns())
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipping_methods" WHERE \("id" = \?\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetShippingMethodByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_Found_ReturnsShippingMethodModel() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), carrier1)
	suite.expectSelectByID(shippingMethod1)

	//act
	shippingMethod, err := suite.repository.GetShippingMethodByID(shippingMethod1.ID)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_CreateShippingMethod_ReturnsCreatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), carrier1)
	suite.mock.
		ExpectExec(`INSERT INTO "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shippingMethod1)

	//act
	shippingMethod, err := suite.repository.CreateShippingMethod(fixtures.GetShippingMethod(uint(0), uint(1), &models.Carrier{}))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.mock.
		ExpectExec(`UPDATE "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_Found_ReturnsUpdatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.mock.
		ExpectExec(`UPDATE "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(shippingMethod1)

	//act
	shippingMethod, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "shipping_methods"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteShippingMethod(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShippingMethodRepositoryTestSuite) Test_DeleteShippingMethod_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "shipping_methods"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteShippingMethod(1)

	//assert
	suite.assert.Nil(err)
}

func (suite *ShippingMethodRepositoryTestSuite) expectSelectByID(shippingMethod *models.ShippingMethod) {
	shippingMethodRows := sqlmock.
		NewRows(fixtures.GetShippingMethodColumns()).
		AddRow(fixtures.GetShippingMethodRow(shippingMethod)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "shipping_methods" WHERE \("id" = \?\) .+`).
		WithArgs(shippingMethod.ID).
		WillReturnRows(shippingMethodRows)
	carrierRows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(&shippingMethod.Carrier)...)
	suite.mock.ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" = \?\)`).
		WithArgs(&shippingMethod.Carrier.ID).
		WillReturnRows(carrierRows)
}
