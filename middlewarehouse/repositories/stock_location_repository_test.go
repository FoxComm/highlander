package repositories

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/suite"
)

type stockLocationRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IStockLocationRepository
	location   *models.StockLocation
}

func TestStockLocationRepositorySuite(t *testing.T) {
	suite.Run(t, new(stockLocationRepositoryTestSuite))
}

func (suite *stockLocationRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewStockLocationRepository(suite.db)
}

func (suite *stockLocationRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"stock_locations",
	})

	// create one stock location for test cases
	suite.location = &models.StockLocation{
		Name:    "Testsss Location",
		Type:    "Warehouse",
		Address: &models.Address{Name: "WH Address"},
	}
	suite.repository.CreateLocation(suite.location)
}

func (suite *stockLocationRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocations() {
	locations, exception := suite.repository.GetLocations()

	suite.Nil(exception)
	suite.Equal(1, len(locations))
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById() {
	location, exception := suite.repository.GetLocationByID(suite.location.ID)

	suite.Nil(exception)
	suite.Equal(suite.location.Name, location.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById_NotFound() {
	location, exception := suite.repository.GetLocationByID(suite.location.ID + 1)

	suite.Nil(location)
	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, suite.location.ID+1).Error(), exception.ToString())
}

func (suite *stockLocationRepositoryTestSuite) Test_CreateLocation() {
	model := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}

	location, exception := suite.repository.CreateLocation(model)

	suite.NotNil(location)
	suite.Nil(exception)
	suite.NotNil(location.ID)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation() {
	model := *suite.location
	model.Name = "Updated Name"

	location, exception := suite.repository.UpdateLocation(&model)

	suite.NotNil(location)
	suite.Nil(exception)
	suite.Equal(model.Name, location.Name)
	suite.NotEqual(suite.location.Name, location.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation_NotFound() {
	model := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}
	model.ID = 100

	location, exception := suite.repository.UpdateLocation(model)

	suite.Nil(location)
	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, 100).Error(), exception.ToString())
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation() {
	exception := suite.repository.DeleteLocation(suite.location.ID)

	suite.Nil(exception)
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation_NotFound() {
	exception := suite.repository.DeleteLocation(100)

	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, 100).Error(), exception.ToString())
}
