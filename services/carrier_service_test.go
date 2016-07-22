package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierServiceTestSuite struct {
	suite.Suite
	service ICarrierService
	db      *gorm.DB
}

func TestCarrierServiceSuite(t *testing.T) {
	suite.Run(t, new(CarrierServiceTestSuite))
}

func (suite *CarrierServiceTestSuite) SetupTest() {
	var err error
	suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	suite.service = NewCarrierService(suite.db)

	tasks.TruncateTables([]string{
		"carriers",
	})
}

func (suite *CarrierServiceTestSuite) TestGetCarriers() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	CreateCarrier(carrier1)
	carrier2 := &payloads.Carrier{"DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	CreateCarrier(carrier2)

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

func (suite *CarrierServiceTestSuite) TestGetCarrierById() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.service.CreateCarrier(carrier1)
	carrier2 := &payloads.Carrier{"DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	suite.service.CreateCarrier(carrier2)
	carriers, err := GetCarriers()

	//act
	carrier, err := suite.service.GetCarrierByID(carriers[1].ID)

	//assert
	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), carrier2.Name, carrier.Name)
	assert.Equal(suite.T(), carrier2.Name, carrier.Name)
}

func (suite *CarrierServiceTestSuite) TestCreaterCarrier() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	payload := &payloads.Carrier{name, trackingTemplate}

	//act
	id, err := suite.service.CreateCarrier(payload)

	//assert
	assert.Nil(suite.T(), err)
	var carrier models.Carrier
	assert.Nil(suite.T(), suite.db.First(&carrier, id).Error)
	assert.Equal(suite.T(), name, carrier.Name)
	assert.Equal(suite.T(), trackingTemplate, carrier.TrackingTemplate)
}

func (suite *CarrierServiceTestSuite) TestUpdateCarrier() {
	//arrange
	name, newName, trackingTemplate := "DHL", "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	id, _ := suite.service.CreateCarrier(&payloads.Carrier{name, trackingTemplate})

	//act
	err := suite.service.UpdateCarrier(id, &payloads.Carrier{newName, trackingTemplate})

	//assert
	assert.Nil(suite.T(), err)
	var carrier models.Carrier
	assert.Nil(suite.T(), suite.db.First(&carrier, id).Error)
	assert.Equal(suite.T(), newName, carrier.Name)
}

func (suite *CarrierServiceTestSuite) TestDeleteCarrier() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.service.CreateCarrier(carrier1)
	carrier2 := &payloads.Carrier{"DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	suite.service.CreateCarrier(carrier2)
	carriers, err := suite.service.GetCarriers()

	//act
	err = suite.service.DeleteCarrier(carriers[0].ID)

	//assert
	assert.Nil(suite.T(), err)
	carriers, err = suite.service.GetCarriers()
	assert.Equal(suite.T(), carrier2.Name, carriers[0].Name)
	assert.Equal(suite.T(), carrier2.TrackingTemplate, carriers[0].TrackingTemplate)
}
