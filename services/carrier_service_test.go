package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierServiceTestSuite struct {
	suite.Suite
	service ICarrierService
	db      *gorm.DB
	mock    sqlmock.Sqlmock
}

func TestCarrierServiceSuite(t *testing.T) {
	suite.Run(t, new(CarrierServiceTestSuite))
}

func (suite *CarrierServiceTestSuite) SetupTest() {
	var err error
	sqldb, mock, err := sqlmock.New()
	db, err := gorm.Open("sqlmock", sqldb)
	suite.db, suite.mock = db, mock

	// suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	suite.service = NewCarrierService(suite.db)

	tasks.TruncateTables([]string{
		"carriers",
	})
}

func (suite *CarrierServiceTestSuite) TearDownSuite() {
	// we make sure that all expectations were met
	assert.Nil(suite.T(), suite.mock.ExpectationsWereMet())

	//make sure all expectations were met
	suite.db.Close()
}

func (suite *CarrierServiceTestSuite) TestGetCarriers() {
	//arrange
	carrier1 := &models.Carrier{
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
	}
	carrier2 := &models.Carrier{
		Name:             "DHL",
		TrackingTemplate: "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL",
	}
	rows := sqlmock.NewRows([]string{"id", "name", "tracking_template"}).
		AddRow(1, carrier1.Name, carrier1.TrackingTemplate).
		AddRow(2, carrier2.Name, carrier2.TrackingTemplate)

	suite.mock.ExpectQuery("SELECT (.+) FROM \"carriers\"").WillReturnRows(rows)

	//act
	carriers, err := suite.service.GetCarriers()

	//assert
	assert.Nil(suite.T(), err)

	assert.Equal(suite.T(), 2, len(carriers))
	assert.Equal(suite.T(), carrier1.Name, carriers[0].Name)
	assert.Equal(suite.T(), carrier1.TrackingTemplate, carriers[0].TrackingTemplate)
	assert.Equal(suite.T(), carrier2.Name, carriers[1].Name)
	assert.Equal(suite.T(), carrier2.TrackingTemplate, carriers[1].TrackingTemplate)
}

// func (suite *CarrierServiceTestSuite) TestGetCarrierById() {
// 	//arrange
// 	carrier1 := &models.Carrier{
// 		Name:             "UPS",
// 		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
// 	}
// 	suite.service.CreateCarrier(carrier1)
// 	carrier2 := &models.Carrier{
// 		Name:             "DHL",
// 		TrackingTemplate: "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL",
// 	}
// 	suite.service.CreateCarrier(carrier2)
// 	carriers, err := suite.service.GetCarriers()

// 	//act
// 	carrier, err := suite.service.GetCarrierByID(carriers[1].ID)

// 	//assert
// 	assert.Nil(suite.T(), err)
// 	assert.Equal(suite.T(), carrier2.Name, carrier.Name)
// 	assert.Equal(suite.T(), carrier2.Name, carrier.Name)
// }

// func (suite *CarrierServiceTestSuite) TestCreaterCarrier() {
// 	//arrange
// 	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
// 	model := &models.Carrier{Name: name, TrackingTemplate: trackingTemplate}

// 	//act
// 	id, err := suite.service.CreateCarrier(model)

// 	//assert
// 	assert.Nil(suite.T(), err)
// 	var carrier models.Carrier
// 	assert.Nil(suite.T(), suite.db.First(&carrier, id).Error)
// 	assert.Equal(suite.T(), name, carrier.Name)
// 	assert.Equal(suite.T(), trackingTemplate, carrier.TrackingTemplate)
// }

// func (suite *CarrierServiceTestSuite) TestUpdateCarrier() {
// 	//arrange
// 	name, newName, trackingTemplate := "DHL", "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
// 	id, _ := suite.service.CreateCarrier(&models.Carrier{Name: name, TrackingTemplate: trackingTemplate})

// 	//act
// 	err := suite.service.UpdateCarrier(&models.Carrier{ID: id, Name: newName, TrackingTemplate: trackingTemplate})

// 	//assert
// 	assert.Nil(suite.T(), err)
// 	var carrier models.Carrier
// 	assert.Nil(suite.T(), suite.db.First(&carrier, id).Error)
// 	assert.Equal(suite.T(), newName, carrier.Name)
// }

// func (suite *CarrierServiceTestSuite) TestDeleteCarrier() {
// 	//arrange
// 	carrier1 := &models.Carrier{
// 		Name:             "UPS",
// 		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
// 	}
// 	suite.service.CreateCarrier(carrier1)
// 	carrier2 := &models.Carrier{
// 		Name:             "DHL",
// 		TrackingTemplate: "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL",
// 	}
// 	suite.service.CreateCarrier(carrier2)
// 	carriers, err := suite.service.GetCarriers()

// 	//act
// 	err = suite.service.DeleteCarrier(carriers[0].ID)

// 	//assert
// 	assert.Nil(suite.T(), err)
// 	carriers, err = suite.service.GetCarriers()
// 	assert.Equal(suite.T(), carrier2.Name, carriers[0].Name)
// 	assert.Equal(suite.T(), carrier2.TrackingTemplate, carriers[0].TrackingTemplate)
// }
