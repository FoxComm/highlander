package repositories

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
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
	suite.db.Close()
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	carrier2 := &models.Carrier{uint(2), "DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	rows := sqlmock.
		NewRows([]string{"id", "name", "tracking_template"}).
		AddRow(carrier1.ID, carrier1.Name, carrier1.TrackingTemplate).
		AddRow(carrier2.ID, carrier2.Name, carrier2.TrackingTemplate)
	suite.mock.ExpectQuery(`SELECT (.+) FROM "carriers"`).WillReturnRows(rows)

	//act
	carriers, err := suite.repository.GetCarriers()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1, carriers[0])
	suite.assert.Equal(carrier2, carriers[1])

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows([]string{"id", "name", "tracking_template"})
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "carriers" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetCarrierByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *CarrierRepositoryTestSuite) Test_GetCarrierById_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	rows := sqlmock.
		NewRows([]string{"id", "name", "tracking_template"}).
		AddRow(carrier1.ID, carrier1.Name, carrier1.TrackingTemplate)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "carriers" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	carrier, err := suite.repository.GetCarrierByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *CarrierRepositoryTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := &models.Carrier{0, "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.mock.
		ExpectExec(`INSERT INTO "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	carrier, err := suite.repository.CreateCarrier(carrier1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), carrier.ID)
	suite.assert.Equal(carrier1, carrier)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.mock.
		ExpectExec(`UPDATE "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	_, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *CarrierRepositoryTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.mock.
		ExpectExec(`UPDATE "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	carrier, err := suite.repository.UpdateCarrier(carrier1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
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

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}
