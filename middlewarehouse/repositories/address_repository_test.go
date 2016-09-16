package repositories

import (
	"fmt"
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/common/db/config"
	"github.com/FoxComm/highlander/middlewarehouse/common/db/tasks"
	"github.com/FoxComm/highlander/middlewarehouse/common/tests"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"

	"github.com/stretchr/testify/suite"
)

type AddressRepositoryTestSuite struct {
	GeneralRepositoryTestSuite
	repository IAddressRepository
	region1    *models.Region
}

func TestAddressRepositorySuite(t *testing.T) {
	suite.Run(t, new(AddressRepositoryTestSuite))
}

func (suite *AddressRepositoryTestSuite) SetupSuite() {
	suite.db = config.TestConnection()

	suite.repository = NewAddressRepository(suite.db)
	suite.region1 = &models.Region{}
	suite.Nil(suite.db.Preload("Country").First(suite.region1, 1).Error)
}

func (suite *AddressRepositoryTestSuite) SetupTest() {
	tasks.TruncateTables(suite.db, []string{
		"addresses",
	})
}

func (suite *AddressRepositoryTestSuite) TearDownSuite() {
	suite.db.Close()
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_NotFound_ReturnsNotFoundError() {
	//act
	_, err := suite.repository.GetAddressByID(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorAddressNotFound, 1), err)
}

func (suite *AddressRepositoryTestSuite) Test_GetAddressByID_Found_ReturnsAddressModel() {
	//arrange
	address1 := fixtures.GetAddress(1, 1, suite.region1)
	suite.Nil(suite.db.Create(address1).Error)

	//act
	address, err := suite.repository.GetAddressByID(address1.ID)

	//assert
	suite.Nil(err)
	tests.SyncDates(address, address1)
	suite.Equal(address1, address)
}

func (suite *AddressRepositoryTestSuite) Test_CreateAddress_ReturnsCreatedRecord() {
	//arrange
	address1 := fixtures.GetAddress(1, 1, suite.region1)

	//act
	address, err := suite.repository.CreateAddress(fixtures.GetAddress(0, 1, &models.Region{}))

	//assert
	suite.Nil(err)
	tests.SyncDates(address, address1)
	suite.Equal(address1, address)
}

func (suite *AddressRepositoryTestSuite) Test_DeleteAddress_NotFound_ReturnsNotFoundError() {
	//act
	err := suite.repository.DeleteAddress(1)

	//assert
	suite.Equal(fmt.Errorf(ErrorAddressNotFound, 1), err)
}

func (suite *AddressRepositoryTestSuite) Test_DeleteAddress_Found_ReturnsNoError() {
	//arrange
	address1 := fixtures.GetAddress(1, 1, suite.region1)
	suite.Nil(suite.db.Create(address1).Error)

	//act
	err := suite.repository.DeleteAddress(1)

	//assert
	suite.Nil(err)
}
