package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierServiceTestSuite struct {
	GeneralServiceTestSuite
	service ICarrierService
}

func TestCarrierServiceSuite(t *testing.T) {
	suite.Run(t, new(CarrierServiceTestSuite))
}

func (suite *CarrierServiceTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.service = NewCarrierService(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *CarrierServiceTestSuite) TearDownTest() {
	// we make sure that all expectations were met
	assert.Nil(suite.T(), suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *CarrierServiceTestSuite) Test_GetCarriers() {
	//arrange
	carrier1 := &models.Carrier{
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
	}
	carrier2 := &models.Carrier{
		Name:             "DHL",
		TrackingTemplate: "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL",
	}
	rows := sqlmock.
		NewRows([]string{"id", "name", "tracking_template"}).
		AddRow(1, carrier1.Name, carrier1.TrackingTemplate).
		AddRow(2, carrier2.Name, carrier2.TrackingTemplate)
	suite.mock.ExpectQuery(`SELECT (.+) FROM "carriers"`).WillReturnRows(rows)

	//act
	carriers, err := suite.service.GetCarriers()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1.Name, carriers[0].Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carriers[0].TrackingTemplate)
	suite.assert.Equal(carrier2.Name, carriers[1].Name)
	suite.assert.Equal(carrier2.TrackingTemplate, carriers[1].TrackingTemplate)
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierById() {
	//arrange
	carrier1 := &models.Carrier{
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
	}
	rows := sqlmock.
		NewRows([]string{"id", "name", "tracking_template"}).
		AddRow(1, carrier1.Name, carrier1.TrackingTemplate)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "carriers" WHERE \("id" = \?\) (.+)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	carrier, err := suite.service.GetCarrierByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1.Name, carrier.Name)
	suite.assert.Equal(carrier1.Name, carrier.Name)
}

func (suite *CarrierServiceTestSuite) Test_CreateCarrier() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	model := &models.Carrier{Name: name, TrackingTemplate: trackingTemplate}
	suite.mock.
		ExpectExec(`INSERT INTO "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	id, err := suite.service.CreateCarrier(model)

	//assert
	suite.assert.Equal(uint(1), id)
	suite.assert.Nil(err)
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	suite.mock.
		ExpectExec(`UPDATE "carriers"`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.service.UpdateCarrier(&models.Carrier{ID: 1, Name: name, TrackingTemplate: trackingTemplate})

	//assert
	suite.assert.Nil(err)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier() {
	//arrange
	suite.mock.
		ExpectExec(`DELETE FROM "carriers"`).
		WithArgs(1).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.service.DeleteCarrier(1)

	//assert
	suite.assert.Nil(err)
}
