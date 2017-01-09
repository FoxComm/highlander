package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
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
	suite.db = config.TestConnection()
	repository := repositories.NewStockLocationRepository(suite.db)
	suite.service = NewStockLocationService(repository)
}

func (suite *StockLocationServiceTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{"stock_locations"})
}

func (suite *StockLocationServiceTestSuite) Test_GetLocations() {
	models := []*models.StockLocation{
		{Name: "Location Name 1", Type: "Warehouse"},
		{Name: "Location Name 2", Type: "Warehouse"},
	}
	suite.Nil(suite.db.Create(models[0]).Error)
	suite.Nil(suite.db.Create(models[1]).Error)

	locations, err := suite.service.GetLocations()

	suite.Nil(err)
	suite.Equal(models, locations)
	suite.repository.AssertExpectations(suite.T())
}

// func (suite *StockLocationServiceTestSuite) Test_GetLocationByID() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.repository.On("GetLocationByID", uint(1)).Return(model, nil).Once()

// 	location, err := suite.service.GetLocationByID(1)

// 	suite.Nil(err)
// 	suite.Equal(model, location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_GetLocationByID_NotFound() {
// 	suite.repository.On("GetLocationByID", uint(1)).Return(nil, errors.New("Error")).Once()

// 	location, err := suite.service.GetLocationByID(1)

// 	suite.NotNil(err)
// 	suite.Nil(location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_CreateLocation() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.repository.On("CreateLocation", model).Return(model, nil).Once()

// 	location, err := suite.service.CreateLocation(model)

// 	suite.Nil(err)
// 	suite.Equal(model, location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_CreateLocation_Error() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.repository.On("CreateLocation", model).Return(nil, errors.New("Error")).Once()

// 	location, err := suite.service.CreateLocation(model)

// 	suite.NotNil(err)
// 	suite.Nil(location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_UpdateLocation() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.repository.On("UpdateLocation", model).Return(model, nil).Once()

// 	location, err := suite.service.UpdateLocation(model)

// 	suite.Nil(err)
// 	suite.Equal(model, location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_UpdateLocation_Error() {
// 	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
// 	suite.repository.On("CreateLocation", model).Return(nil, errors.New("Error")).Once()

// 	location, err := suite.service.CreateLocation(model)

// 	suite.NotNil(err)
// 	suite.Nil(location)
// 	suite.repository.AssertExpectations(suite.T())
// }

// func (suite *StockLocationServiceTestSuite) Test_DeleteLocation() {
// 	suite.repository.On("DeleteLocation", uint(1)).Return(nil).Once()

// 	err := suite.service.DeleteLocation(1)

// 	suite.Nil(err)
// }

// func (suite *StockLocationServiceTestSuite) Test_DeleteLocation_Error() {
// 	suite.repository.On("DeleteLocation", uint(1)).Return(errors.New("Error")).Once()

// 	err := suite.service.DeleteLocation(1)

// 	suite.NotNil(err)
// }
