package repositories

import (
	"database/sql"
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
		NewRows([]string{"id", "name", "region_id", "city", "zip", "address1", "address2", "phone_number", "created_at", "updated_at", "deleted_at"})
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "addresses" WHERE (.+) \(\("id" = \?\)\) (.+)`).
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
	address1 := &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231", "Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
	rows := sqlmock.
		NewRows([]string{"id", "name", "region_id", "city", "zip", "address1", "address2", "phone_number", "created_at", "updated_at", "deleted_at"}).
		AddRow(address1.ID, address1.Name, address1.RegionID, address1.City, address1.Zip, address1.Address1,
			nil, address1.PhoneNumber, address1.CreatedAt, address1.UpdatedAt, address1.DeletedAt)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "addresses" WHERE (.+) \(\("id" = \?\)\) (.+)`).
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
	address1 := &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231", "Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
	suite.mock.
		ExpectExec(`INSERT INTO "addresses"`).
		WillReturnResult(sqlmock.NewResult(1, 1))
	rows := sqlmock.
		NewRows([]string{"id", "name", "region_id", "city", "zip", "address1", "address2", "phone_number", "created_at", "updated_at", "deleted_at"}).
		AddRow(address1.ID, address1.Name, address1.RegionID, address1.City, address1.Zip, address1.Address1,
			nil, address1.PhoneNumber, address1.CreatedAt, address1.UpdatedAt, address1.DeletedAt)
	suite.mock.
		ExpectQuery(`SELECT (.+) FROM "addresses" WHERE (.+) \(\("id" = \?\)\) (.+)`).
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
