package repositories

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/fixtures"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository ICarrierRepository
}

func TestCarrierRepositorySuite(t *testing.T) {
	suite.Run(t, new(CarrierRepositoryTestSuite))
}

func (suite *CarrierRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.repository = NewCarrierRepository(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *CarrierRepositoryTestSuite) TearDownTest() {
	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
	suite.db.Close()
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	carrier2 := fixtures.GetCarrier(uint(2))
	rows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(carrier1)...).
		AddRow(fixtures.GetCarrierRow(carrier2)...)
	suite.mock.ExpectQuery(`SELECT .+ FROM "carriers"`).WillReturnRows(rows)

	//act
	carriers, err := suite.repository.GetCarriers()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1, carriers[0])
	suite.assert.Equal(carrier2, carriers[1])
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(fixtures.GetCarrierColumns())
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" = \?\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetCarrierByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierByID_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.expectSelectByID(carrier1)

	//act
	carrier, err := suite.repository.GetCarrierByID(carrier1.ID)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.mock.
		ExpectExec(`INSERT INTO "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(carrier1)

	//act
	carrier, err := suite.repository.CreateCarrier(fixtures.GetCarrier(uint(0)))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.mock.
		ExpectExec(`UPDATE "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.mock.
		ExpectExec(`UPDATE "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(carrier1)

	//act
	carrier, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)
}

func (suite *CarrierRepositoryTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "carriers"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteCarrier(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *CarrierRepositoryTestSuite) Test_DeleteCarrier_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "carriers"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteCarrier(1)

	//assert
	suite.assert.Nil(err)
}

func (suite *CarrierRepositoryTestSuite) expectSelectByID(carrier *models.Carrier) {
	rows := sqlmock.
		NewRows(fixtures.GetCarrierColumns()).
		AddRow(fixtures.GetCarrierRow(carrier)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "carriers" WHERE \("id" = \?\) .+`).
		WithArgs(carrier.ID).
		WillReturnRows(rows)
}
