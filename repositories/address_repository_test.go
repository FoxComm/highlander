package repositories

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type AddressRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IAddressRepository
}

func TestAddressRepositorySuite(t *testing.T) {
	suite.Run(t, new(AddressRepositoryTestSuite))
}

func (suite *AddressRepositoryTestSuite) SetupTest() {
	suite.db, suite.mock = CreateDbMock()

	suite.repository = NewAddressRepository(suite.db)

	suite.assert = assert.New(suite.T())
}

func (suite *AddressRepositoryTestSuite) TearDownTest() {
	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())

	suite.db.Close()
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(fixtures.GetAddressColumns())
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetAddressByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_Found_ReturnsAddressModel() {
	//arrange
	country1 := fixtures.GetCountry(uint(1))
	region1 := fixtures.GetRegion(uint(1), uint(1), country1)
	address1 := fixtures.GetAddress(uint(1), uint(1), region1)
	suite.expectSelectByID(address1)

	//act
	address, err := suite.repository.GetAddressByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(address1, address)
}

func (suite *AddressRepositoryTestSuite) Test_CreateAddress_ReturnsCreatedRecord() {
	//arrange
	country1 := fixtures.GetCountry(uint(1))
	region1 := fixtures.GetRegion(uint(1), uint(1), country1)
	address1 := fixtures.GetAddress(uint(1), uint(1), region1)
	suite.mock.
		ExpectExec(`INSERT INTO "addresses"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	suite.expectSelectByID(address1)

	//act
	address, err := suite.repository.CreateAddress(fixtures.GetAddress(uint(0), uint(1), &models.Region{}))

	//assert
	suite.assert.Nil(err)
	address1.CreatedAt = address.CreatedAt
	address1.UpdatedAt = address.UpdatedAt
	suite.assert.Equal(address1, address)
}


func (suite *AddressRepositoryTestSuite) Test_DeleteAddress_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "addresses" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 0))

	//act
	err := suite.repository.DeleteAddress(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *AddressRepositoryTestSuite) Test_DeleteAddress_Found_ReturnsNoError() {
	//arrange
	suite.mock.
		ExpectExec(`UPDATE "addresses" SET deleted_at=\? .+ \(\("id" = \?\)\)`).
		WillReturnResult(sqlmock.NewResult(1, 1))

	//act
	err := suite.repository.DeleteAddress(1)

	//assert
	suite.assert.Nil(err)
}

func (suite *AddressRepositoryTestSuite) expectSelectByID(address *models.Address) {
	addressRows := sqlmock.
		NewRows(fixtures.GetAddressColumns()).
		AddRow(fixtures.GetAddressRow(address)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(address.ID).
		WillReturnRows(addressRows)
	regionRows := sqlmock.
		NewRows(fixtures.GetRegionColumns()).
		AddRow(fixtures.GetRegionRow(&address.Region)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "regions" WHERE \("id" = \?\)`).
		WithArgs(address.Region.ID).
		WillReturnRows(regionRows)
	countryRows := sqlmock.
		NewRows(fixtures.GetCountryColumns()).
		AddRow(fixtures.GetCountryRow(&address.Region.Country)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "countries" WHERE \("id" = \?\)`).
		WithArgs(address.Region.Country.ID).
		WillReturnRows(countryRows)
}
