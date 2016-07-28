package services

import (
	"errors"
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type StockLocationServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.StockLocationRepositoryMock
	service    IStockLocationService
}

func TestStockLocationServiceSuite(t *testing.T) {
	suite.Run(t, new(StockLocationServiceTestSuite))
}

func (suite *StockLocationServiceTestSuite) SetupSuite() {
	suite.repository = &mocks.StockLocationRepositoryMock{}
	suite.service = NewStockLocationService(suite.repository)

	suite.assert = assert.New(suite.T())
}

func (suite *StockLocationServiceTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *StockLocationServiceTestSuite) Test_GetLocations() {
	models := []*models.StockLocation{
		{Name: "Location Name 1", Type: "Warehouse"},
		{Name: "Location Name 2", Type: "Warehouse"},
	}
	suite.repository.On("GetLocations").Return(models, nil).Once()

	locations, err := suite.service.GetLocations()

	suite.assert.Nil(err)
	suite.assert.Equal(models, locations)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("GetLocationByID", uint(1)).Return(model, nil).Once()

	location, err := suite.service.GetLocationByID(1)

	suite.assert.Nil(err)
	suite.assert.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID_NotFound() {
	suite.repository.On("GetLocationByID", uint(1)).Return(nil, errors.New("Error")).Once()

	location, err := suite.service.GetLocationByID(1)

	suite.assert.NotNil(err)
	suite.assert.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("CreateLocation", model).Return(model, nil).Once()

	location, err := suite.service.CreateLocation(model)

	suite.assert.Nil(err)
	suite.assert.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("CreateLocation", model).Return(nil, errors.New("Error")).Once()

	location, err := suite.service.CreateLocation(model)

	suite.assert.NotNil(err)
	suite.assert.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("UpdateLocation", model).Return(model, nil).Once()

	location, err := suite.service.UpdateLocation(model)

	suite.assert.Nil(err)
	suite.assert.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("CreateLocation", model).Return(nil, errors.New("Error")).Once()

	location, err := suite.service.CreateLocation(model)

	suite.assert.NotNil(err)
	suite.assert.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation() {
	suite.repository.On("DeleteLocation", uint(1)).Return(nil).Once()

	err := suite.service.DeleteLocation(1)

	suite.assert.Nil(err)
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation_Error() {
	suite.repository.On("DeleteLocation", uint(1)).Return(errors.New("Error")).Once()

	err := suite.service.DeleteLocation(1)

	suite.assert.NotNil(err)
}
