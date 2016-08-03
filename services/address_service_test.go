package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"database/sql"
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

	suite.assert = assert.New(suite.T())
}

func (suite *AddressServiceTestSuite) TearDownTest() {
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
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *AddressServiceTestSuite) Test_GetAddressByID_Found_ReturnsAddressModel() {
	//arrange
	address1 := suite.getTestAddress1()
	suite.repository.On("GetAddressByID", uint(1)).Return(address1, nil).Once()

	//act
	address, err := suite.service.GetAddressByID(uint(1))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(address1, address)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *AddressServiceTestSuite) Test_CreateAddress_ReturnsCreatedRecord() {
	//arrange
	address1 := suite.getTestAddress1()
	suite.repository.On("CreateAddress", address1).Return(address1, nil).Once()

	//act
	address, err := suite.service.CreateAddress(address1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(address1, address)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *AddressServiceTestSuite) getTestAddress1() *models.Address {
	return &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231",
		"Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
}