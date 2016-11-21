package services

import (
	"errors"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
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

	suite.Nil(err)
	suite.Equal(models, locations)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("GetLocationByID", uint(1)).Return(model, nil).Once()

	location, err := suite.service.GetLocationByID(1)

	suite.Nil(err)
	suite.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID_NotFound() {
	ex := repositories.NewEntityNotFoundException(repositories.StockLocationEntity, "1", fmt.Errorf(repositories.ErrorStockLocationNotFound, 1))
	suite.repository.On("GetLocationByID", uint(1)).Return(nil, ex).Once()

	location, err := suite.service.GetLocationByID(1)

	suite.Equal(ex, err)
	suite.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("CreateLocation", model).Return(model, nil).Once()

	location, err := suite.service.CreateLocation(model)

	suite.Nil(err)
	suite.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	ex := repositories.NewDatabaseException(errors.New("Failure"))
	suite.repository.On("CreateLocation", model).Return(nil, ex).Once()

	location, err := suite.service.CreateLocation(model)

	suite.Equal(ex, err)
	suite.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.repository.On("UpdateLocation", model).Return(model, nil).Once()

	location, err := suite.service.UpdateLocation(model)

	suite.Nil(err)
	suite.Equal(model, location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	ex := repositories.NewEntityNotFoundException(repositories.StockLocationEntity, "1", fmt.Errorf(repositories.ErrorStockLocationNotFound, 1))
	suite.repository.On("CreateLocation", model).Return(nil, ex).Once()

	location, err := suite.service.CreateLocation(model)

	suite.Equal(ex, err)
	suite.Nil(location)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation() {
	suite.repository.On("DeleteLocation", uint(1)).Return(nil).Once()

	err := suite.service.DeleteLocation(1)

	suite.Nil(err)
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation_Error() {
	ex := repositories.NewEntityNotFoundException(repositories.StockLocationEntity, "1", fmt.Errorf(repositories.ErrorStockLocationNotFound, 1))
	suite.repository.On("DeleteLocation", uint(1)).Return(ex).Once()

	err := suite.service.DeleteLocation(1)

	suite.Equal(ex, err)
}
