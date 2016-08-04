package repositories

import (
	"database/sql"
	"database/sql/driver"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
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
	suite.db.Close()
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_NotFound_ReturnsNotFoundError() {
	//arrange
	rows := sqlmock.
		NewRows(suite.getAddressColumns())
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	_, err := suite.repository.GetAddressByID(1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_Found_ReturnsAddressModel() {
	//arrange
	address1 := suite.getTestAddress1()
	rows := sqlmock.
		NewRows(suite.getAddressColumns()).
		AddRow(suite.getAddressRow(address1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	address, err := suite.repository.GetAddressByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(address1, address)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *AddressRepositoryTestSuite) Test_CreateAddress_ReturnsCreatedRecord() {
	//arrange
	address1 := suite.getTestAddress1()
	suite.mock.
		ExpectExec(`INSERT INTO "addresses"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows(suite.getAddressColumns()).
		AddRow(suite.getAddressRow(address1)...)
	suite.mock.
		ExpectQuery(`SELECT .+ FROM "addresses" WHERE .+ \(\("id" = \?\)\) .+`).
		WithArgs(1).
		WillReturnRows(rows)

	//act
	address, err := suite.repository.CreateAddress(address1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(uint(1), address.ID)
	address1.CreatedAt = address.CreatedAt
	address1.UpdatedAt = address.UpdatedAt
	suite.assert.Equal(address1, address)

	//make sure that all expectations were met
	suite.assert.Nil(suite.mock.ExpectationsWereMet())
}

func (suite *AddressRepositoryTestSuite) getTestAddress1() *models.Address {
	return &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231",
		"Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
}

func (suite *AddressRepositoryTestSuite) getAddressColumns() []string {
	return []string{"id", "name", "region_id", "city", "zip", "address1", "address2",
		"phone_number", "created_at", "updated_at", "deleted_at"}
}

func (suite *AddressRepositoryTestSuite) getAddressRow(address *models.Address) []driver.Value {
	return []driver.Value{address.ID, address.Name, address.RegionID, address.City, address.Zip, address.Address1,
		nil, address.PhoneNumber, address.CreatedAt, address.UpdatedAt, address.DeletedAt}
}
