package repositories

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/models"

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
	suite.db.Close()
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 days ground"}
	shippingMethod2 := &models.ShippingMethod{uint(2), uint(2), "DHL 2 days ground"}
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(shippingMethod1.ID, shippingMethod1.CarrierID, shippingMethod1.Name).
		AddRow(shippingMethod2.ID, shippingMethod2.CarrierID, shippingMethod2.Name)
	suite.mock.ExpectQuery(`SELECT (.+) FROM "shipping_methods"`).WillReturnRows(rows)

	//act
	shippingMethods, err := suite.repository.GetShippingMethods()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(shippingMethods))
	suite.assert.Equal(shippingMethod1, shippingMethods[0])
	suite.assert.Equal(shippingMethod2, shippingMethods[1])

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"})
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "shipping_methods" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetShippingMethodByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShippingMethodRepositoryTestSuite) Test_GetShippingMethodByID_Found_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 days ground"}
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(shippingMethod1.ID, shippingMethod1.CarrierID, shippingMethod1.Name)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "shipping_methods" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shippingMethod, err := suite.repository.GetShippingMethodByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1, shippingMethod)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShippingMethodRepositoryTestSuite) Test_CreaterShippingMethod_ReturnsCreatedRecord() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{0, uint(1), "UPS 2 days ground"}
	suite.mock.
		ExpectExec(`INSERT INTO "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(uint(1), shippingMethod1.CarrierID, shippingMethod1.Name)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "shipping_methods" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shippingMethod, err := suite.repository.CreateShippingMethod(shippingMethod1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), shippingMethod.ID)
	suite.assert.Equal(shippingMethod1, shippingMethod)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 days ground"}
	suite.mock.
		ExpectExec(`UPDATE "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *ShippingMethodRepositoryTestSuite) Test_UpdateShippingMethod_Found_ReturnsUpdatedRecord() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{uint(1), uint(1), "UPS 2 days ground"}
	suite.mock.
		ExpectExec(`UPDATE "shipping_methods"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows([]string{"id", "carrier_id", "name"}).
		AddRow(shippingMethod1.ID, shippingMethod1.CarrierID, shippingMethod1.Name)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "shipping_methods" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	shippingMethod, err := suite.repository.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1, shippingMethod)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}
