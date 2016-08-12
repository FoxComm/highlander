package repositories

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
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
	suite.db, _ = config.DefaultConnection()
	suite.repository = NewStockLocationRepository(suite.db)
	suite.assert = assert.New(suite.T())
}
func (suite *stockLocationRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
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

	suite.assert.Nil(err)
	suite.assert.Equal(1, len(locations))
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById() {
	location, err := suite.repository.GetLocationByID(suite.location.ID)

	suite.assert.Nil(err)
	suite.assert.Equal(suite.location.Name, location.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_GetLocationById_NotFound() {
	location, err := suite.repository.GetLocationByID(suite.location.ID + 1)

	suite.assert.Nil(location)
	suite.assert.NotNil(err)
	suite.assert.IsType(gorm.ErrRecordNotFound, err)
}

func (suite *stockLocationRepositoryTestSuite) Test_CreateLocation() {
	model := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}

	location, err := suite.repository.CreateLocation(model)

	suite.assert.NotNil(location)
	suite.assert.Nil(err)
	suite.assert.NotNil(location.ID)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation() {
	model := *suite.location
	model.Name = "Updated Name"

	location, err := suite.repository.UpdateLocation(&model)

	suite.assert.NotNil(location)
	suite.assert.Nil(err)
	suite.assert.Equal(model.Name, location.Name)
	suite.assert.NotEqual(suite.location.Name, location.Name)
}

func (suite *stockLocationRepositoryTestSuite) Test_UpdateLocation_NotFound() {
	model := &models.StockLocation{
		Name:    "Some Name",
		Type:    "Warehouse",
		Address: &models.Address{Name: "Warehouse Address"},
	}
	model.ID = 1e9

	location, err := suite.repository.UpdateLocation(model)

	suite.assert.Nil(location)
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation() {
	err := suite.repository.DeleteLocation(suite.location.ID)

	suite.assert.Nil(err)
}

func (suite *stockLocationRepositoryTestSuite) Test_DeleteLocation_NotFound() {
	err := suite.repository.DeleteLocation(uint(1e9))

	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}
