package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/jinzhu/gorm"
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
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	carrier2 := &models.Carrier{uint(2), "DHL", "http://www.dhl.com/en/express/tracking.shtml?AWB=$number&brand=DHL"}
	suite.repository.On("GetCarriers").Return([]*models.Carrier{carrier1, carrier2}, nil).Once()

	//act
	carriers, err := suite.service.GetCarriers()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(carriers))
	suite.assert.Equal(carrier1, carriers[0])
	suite.assert.Equal(carrier2, carriers[1])

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierById_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("GetCarrierByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierByID_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.repository.On("GetCarrierByID", uint(1)).Return(carrier1, nil).Once()

	//act
	carrier, err := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.repository.On("CreateCarrier", carrier1).Return(carrier1, nil).Once()

	//act
	carrier, err := suite.service.CreateCarrier(carrier1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.repository.On("UpdateCarrier", carrier1).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := &models.Carrier{uint(1), "UPS", "https://wwwapps.ups.com/tracking/tracking.cgi?tracknum=$number"}
	suite.repository.On("UpdateCarrier", carrier1).Return(carrier1, nil).Once()

	//act
	carrier, err := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(carrier1, carrier)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("DeleteCarrier", uint(1)).Return(false, gorm.ErrRecordNotFound).Once()

	//act
	err := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteCarrier", uint(1)).Return(true).Once()

	//act
	err := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.assert.Nil(err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}
