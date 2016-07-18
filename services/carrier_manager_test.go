package services

import (
	"fmt"
	"testing"

	"github.com/FoxComm/middlewarehouse/api/payloads"
	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type CarrierManagerTestSuite struct {
	suite.Suite
	db *gorm.DB
}

func TestCarrierManagerSuite(t *testing.T) {
	suite.Run(t, new(CarrierManagerTestSuite))
}

func (suite *CarrierManagerTestSuite) SetupTest() {
	var err error
	suite.db, err = config.DefaultConnection()
	assert.Nil(suite.T(), err)

	tasks.TruncateTables([]string{
		"carriers",
	})
}

func (suite *CarrierManagerTestSuite) TestGetCarriers() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	CreateCarrier(carrier1)
	carrier2 := &payloads.Carrier{"DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	CreateCarrier(carrier2)

	//act
	carriers, err := GetCarriers()

	//assert
	if !assert.Nil(suite.T(), err) {
		return
	}

	assert.Equal(suite.T(), 2, len(carriers))
	assert.Equal(suite.T(), carrier1.Name, carriers[0].Name)
	assert.Equal(suite.T(), carrier1.TrackingTemplate, carriers[0].TrackingTemplate)
	assert.Equal(suite.T(), carrier2.Name, carriers[1].Name)
	assert.Equal(suite.T(), carrier2.TrackingTemplate, carriers[1].TrackingTemplate)
}

func (suite *CarrierManagerTestSuite) TestCreaterCarrier() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	payload := &payloads.Carrier{name, trackingTemplate}

	//act
	err := CreateCarrier(payload)

	//assert
	fmt.Println(err)
	assert.Nil(suite.T(), err)
	var carrier models.Carrier
	assert.Nil(suite.T(), suite.db.Where("name=?", name).First(&carrier).Error)
	assert.Equal(suite.T(), name, carrier.Name)
	assert.Equal(suite.T(), trackingTemplate, carrier.TrackingTemplate)
}

func (suite *CarrierManagerTestSuite) TestUpdateCarrier() {
	//arrange
	name, newName, trackingTemplate := "DHL", "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	CreateCarrier(&payloads.Carrier{name, trackingTemplate})

	//act
	err := UpdateCarrier(&payloads.Carrier{newName, trackingTemplate})

	//assert
	assert.Nil(suite.T(), err)
	var carrier models.Carrier
	assert.Nil(suite.T(), suite.db.Where("tracking_template=?", trackingTemplate).First(&carrier).Error)
	assert.Equal(suite.T(), newName, carrier.Name)
}

func (suite *CarrierManagerTestSuite) TestDeleteCarrier() {
	//arrange
	carrier1 := &payloads.Carrier{"UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	CreateCarrier(carrier1)
	carrier2 := &payloads.Carrier{"DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	CreateCarrier(carrier2)
	carriers, err := GetCarriers()

	//act
	err = DeleteCarrier(carriers[0].ID)

	//assert
	assert.Nil(suite.T(), err)
	carriers, err = GetCarriers()
	assert.Equal(suite.T(), carrier2.Name, carriers[0].Name)
	assert.Equal(suite.T(), carrier2.TrackingTemplate, carriers[0].TrackingTemplate)
}
