package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type CarrierServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.CarrierRepositoryMock
	service    ICarrierService
}

func TestCarrierServiceSuite(t *testing.T) {
	suite.Run(t, new(CarrierServiceTestSuite))
}

func (suite *CarrierServiceTestSuite) SetupTest() {
	suite.repository = &mocks.CarrierRepositoryMock{}
	suite.service = NewCarrierService(suite.repository)

	suite.assert = assert.New(suite.T())
}

func (suite *CarrierServiceTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}
func (suite *CarrierServiceTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := &models.Carrier{
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
	}
	carrier2 := &models.Carrier{
		Name:             "DHL",
		TrackingTemplate: "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL",
	}
	suite.repository.On("GetCarriers").Return([]*models.Carrier{
		carrier1,
		carrier2,
	}, nil).Once()

	//act
	carriers, err := suite.service.GetCarriers()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1.Name, carriers[0].Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carriers[0].TrackingTemplate)
	suite.assert.Equal(carrier2.Name, carriers[1].Name)
	suite.assert.Equal(carrier2.TrackingTemplate, carriers[1].TrackingTemplate)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierByID_ReturnsCarrierModel() {
	//arrange
	carrier1 := &models.Carrier{
		Name:             "UPS",
		TrackingTemplate: "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number",
	}
	suite.repository.On("GetCarrierByID").Return(carrier1, nil).Once()

	//act
	carrier, err := suite.service.GetCarrierByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1.Name, carrier.Name)
	suite.assert.Equal(carrier1.TrackingTemplate, carrier.TrackingTemplate)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_CreaterCarrier_ReturnsIdOfCreatedRecord() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	model := &models.Carrier{Name: name, TrackingTemplate: trackingTemplate}
	suite.repository.On("CreateCarrier").Return(uint(1), nil).Once()

	//act
	id, err := suite.service.CreateCarrier(model)

	//assert
	suite.assert.Equal(uint(1), id)
	suite.assert.Nil(err)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_ReturnsNoError() {
	//arrange
	name, trackingTemplate := "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"
	suite.repository.On("UpdateCarrier").Return(true).Once()

	//act
	err := suite.service.UpdateCarrier(&models.Carrier{ID: 1, Name: name, TrackingTemplate: trackingTemplate})

	//assert
	suite.assert.Nil(err)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteCarrier").Return(true).Once()

	//act
	err := suite.service.DeleteCarrier(1)

	//assert
	suite.assert.Nil(err)
}
