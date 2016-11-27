package services

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

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
}

func (suite *CarrierServiceTestSuite) TearDownTest() {
	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())

	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}
func (suite *CarrierServiceTestSuite) Test_GetCarriers_ReturnsCarrierModels() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	carrier2 := fixtures.GetCarrier(uint(2))
	suite.repository.On("GetCarriers").Return([]*models.Carrier{carrier1, carrier2}, nil).Once()

	//act
	carriers, exception := suite.service.GetCarriers()

	//assert
	suite.Nil(exception)

	suite.Equal(2, len(carriers))
	suite.Equal(carrier1, carriers[0])
	suite.Equal(carrier2, carriers[1])
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierById_NotFound_ReturnsNotFoundError() {
	//arrange
	ex := repositories.NewEntityNotFoundException(repositories.CarrierEntity, "1", fmt.Errorf(repositories.ErrorCarrierNotFound, 1))
	suite.repository.On("GetCarrierByID", uint(1)).
		Return(nil, ex).Once()

	//act
	_, exception := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.Equal(ex, exception)
}

func (suite *CarrierServiceTestSuite) Test_GetCarrierByID_Found_ReturnsCarrierModel() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.repository.On("GetCarrierByID", uint(1)).Return(carrier1, nil).Once()

	//act
	carrier, exception := suite.service.GetCarrierByID(uint(1))

	//assert
	suite.Nil(exception)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_CreateCarrier_ReturnsCreatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.repository.On("CreateCarrier", carrier1).Return(carrier1, nil).Once()

	//act
	carrier, exception := suite.service.CreateCarrier(carrier1)

	//assert
	suite.Nil(exception)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	ex := repositories.NewEntityNotFoundException(repositories.CarrierEntity, "1", fmt.Errorf(repositories.ErrorCarrierNotFound, 1))
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.repository.On("UpdateCarrier", carrier1).
		Return(nil, ex).Once()

	//act
	_, exception := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.Equal(ex, exception)
}

func (suite *CarrierServiceTestSuite) Test_UpdateCarrier_Found_ReturnsUpdatedRecord() {
	//arrange
	carrier1 := fixtures.GetCarrier(uint(1))
	suite.repository.On("UpdateCarrier", carrier1).Return(carrier1, nil).Once()

	//act
	carrier, exception := suite.service.UpdateCarrier(carrier1)

	//assert
	suite.Nil(exception)
	suite.Equal(carrier1, carrier)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_NotFound_ReturnsNotFoundError() {
	//arrange
	ex := repositories.NewEntityNotFoundException(repositories.CarrierEntity, "1", fmt.Errorf(repositories.ErrorCarrierNotFound, 1))
	suite.repository.On("DeleteCarrier", uint(1)).Return(ex).Once()

	//act
	exception := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.Equal(ex, exception)
}

func (suite *CarrierServiceTestSuite) Test_DeleteCarrier_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteCarrier", uint(1)).Return(nil).Once()

	//act
	exception := suite.service.DeleteCarrier(uint(1))

	//assert
	suite.Nil(exception)
}
