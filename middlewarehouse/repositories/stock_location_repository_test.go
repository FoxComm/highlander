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
	locations, err := suite.repository.GetLocations()

	suite.Nil(err)
	suite.Equal(1, len(locations))
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById() {
	location, err := suite.repository.GetLocationByID(suite.location.ID)

	suite.Nil(err)
	suite.Equal(suite.location.Name, location.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById_NotFound() {
	location, err := suite.repository.GetLocationByID(suite.location.ID + 1)

	suite.Nil(location)
	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, suite.location.ID+1).Error(), err.Error())
}

func (suite *stockLocationRepositoryTestSuite) Test_CreateLocation() {
	location := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}

	err := suite.repository.CreateLocation(location)

	suite.Nil(err)
	suite.NotNil(location.ID)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation() {
	model := *suite.location
	model.Name = "Updated Name"

	err := suite.repository.UpdateLocation(&model)

	suite.Nil(err)
	suite.Equal("Updated Name", model.Name)
	suite.NotEqual(suite.location.Name, model.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation_NotFound() {
	model := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}
	model.ID = 100

	err := suite.repository.UpdateLocation(model)

	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, 100).Error(), err.Error())
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation() {
	err := suite.repository.DeleteLocation(suite.location.ID)

	suite.Nil(err)
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation_NotFound() {
	err := suite.repository.DeleteLocation(100)

	suite.Equal(fmt.Errorf(ErrorStockLocationNotFound, 100).Error(), err.Error())
}
