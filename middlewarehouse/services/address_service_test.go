package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type AddressServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.AddressRepositoryMock
	service    IAddressService
}

func TestAddressServiceSuite(t *testing.T) {
	suite.Run(t, new(AddressServiceTestSuite))
}

func (suite *AddressServiceTestSuite) SetupTest() {
	suite.repository = &mocks.AddressRepositoryMock{}
	suite.service = NewAddressService(suite.repository)
}

func (suite *AddressServiceTestSuite) TearDownTest() {
	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())

	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *AddressServiceTestSuite) Test_GetAddressById_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("GetAddressByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.GetAddressByID(uint(1))

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *AddressServiceTestSuite) Test_GetAddressByID_Found_ReturnsAddressModel() {
	//arrange
	address1 := fixtures.GetAddress(uint(1), uint(1), &models.Region{ID: uint(1)})
	suite.repository.On("GetAddressByID", address1.ID).Return(address1, nil).Once()

	//act
	address, err := suite.service.GetAddressByID(address1.ID)

	//assert
	suite.Nil(err)
	suite.Equal(address1, address)
}

func (suite *AddressServiceTestSuite) Test_CreateAddress_ReturnsCreatedRecord() {
	//arrange
	address1 := fixtures.GetAddress(uint(1), uint(1), &models.Region{ID: uint(1)})
	suite.repository.On("CreateAddress", address1).Return(address1, nil).Once()

	//act
	address, err := suite.service.CreateAddress(address1)

	//assert
	suite.Nil(err)
	suite.Equal(address1, address)
}

func (suite *AddressServiceTestSuite) Test_DeleteAddress_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("DeleteAddress", uint(1)).Return(gorm.ErrRecordNotFound).Once()

	//act
	err := suite.service.DeleteAddress(uint(1))

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *AddressServiceTestSuite) Test_DeleteAddress_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteAddress", uint(1)).Return(nil).Once()

	//act
	err := suite.service.DeleteAddress(uint(1))

	//assert
	suite.Nil(err)
}
