package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
	"github.com/stretchr/testify/suite"
)

type StockLocationServiceTestSuite struct {
	GeneralServiceTestSuite
	service IStockLocationService
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
	suite.Equal(models[0].Name, locations[0].Name)
	suite.Equal(models[0].Type, locations[0].Type)
	suite.Equal(models[1].Name, locations[1].Name)
	suite.Equal(models[1].Type, locations[1].Type)
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)

	location, err := suite.service.GetLocationByID(1)

	suite.Nil(err)
	suite.Equal(model.Name, location.Name)
	suite.Equal(model.Type, location.Type)
}

func (suite *StockLocationServiceTestSuite) Test_GetLocationByID_NotFound() {
	location, err := suite.service.GetLocationByID(1)

	suite.NotNil(err)
	suite.Nil(location)
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}

	location, err := suite.service.CreateLocation(model)

	suite.Nil(err)
	suite.Equal(model.Name, location.Name)
	suite.Equal(model.Type, location.Type)
}

func (suite *StockLocationServiceTestSuite) Test_CreateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Wherehouse"}

	location, err := suite.service.CreateLocation(model)

	suite.NotNil(err)
	suite.Nil(location)
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)
	model.Name = "New Location Name"

	location, err := suite.service.UpdateLocation(model)

	suite.Nil(err)
	suite.Equal(model.Name, location.Name)
	suite.Equal(model.Type, location.Type)
}

func (suite *StockLocationServiceTestSuite) Test_UpdateLocation_Error() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Wherehouse"}

	location, err := suite.service.CreateLocation(model)

	suite.NotNil(err)
	suite.Nil(location)
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation() {
	model := &models.StockLocation{Name: "Location Name 1", Type: "Warehouse"}
	suite.Nil(suite.db.Create(model).Error)

	err := suite.service.DeleteLocation(model.ID)

	suite.Nil(err)
}

func (suite *StockLocationServiceTestSuite) Test_DeleteLocation_Error() {
	suite.NotNil(suite.service.DeleteLocation(1))
}
