package repositories

import (
	"database/sql/driver"
	"testing"

	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"github.com/jinzhu/gorm"
)

type RegionRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IRegionRepository
}

func TestRegionRepositorySuite(t *testing.T) {
	suite.Run(t, new(RegionRepositoryTestSuite))
}

func (suite *RegionRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()
	//suite.db, _ = config.DefaultConnection()

	suite.repository = NewRegionRepository(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *RegionRepositoryTestSuite) TearDownTest() {
	suite.db.Close()
}

func (suite *RegionRepositoryTestSuite) Test_GetRegionByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(suite.getRegionColumns())
	suite.mock.
		ExpectQuery(`SELECT regions.id as id, regions.name as name, countries.id as country_id, countries.name as country_name FROM "regions" .+ WHERE \(regions\.id=\?\)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetRegionByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *RegionRepositoryTestSuite) Test_GetRegionByID_Found_ReturnsRegionModel() {
	//arrange
	region1 := suite.getTestRegion1()
	rows := sqlmock.
		NewRows(suite.getRegionColumns()).
		AddRow(suite.getRegionRow(region1)...)
	suite.mock.
		ExpectQuery(`SELECT regions.id as id, regions.name as name, countries.id as country_id, countries.name as country_name FROM "regions" .+ WHERE \(regions\.id=\?\)`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	region, err := suite.repository.GetRegionByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(region1, region)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *RegionRepositoryTestSuite) getTestRegion1() *models.Region {
	return &models.Region{uint(1), "Home region", uint(1), "My Country"}
}

func (suite *RegionRepositoryTestSuite) getRegionColumns() []string {
	return []string{"id", "name", "country_id", "country_name"}
}

func (suite *RegionRepositoryTestSuite) getRegionRow(region *models.Region) []driver.Value {
	return []driver.Value{region.ID, region.Name, region.CountryID, region.CountryName}
}
