package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type RegionServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.RegionRepositoryMock
	service    IRegionService
}

func TestRegionServiceSuite(t *testing.T) {
	suite.Run(t, new(RegionServiceTestSuite))
}

func (suite *RegionServiceTestSuite) SetupTest() {
	suite.repository = &mocks.RegionRepositoryMock{}
	suite.service = NewRegionService(suite.repository)

	suite.assert = assert.New(suite.T())
}

func (suite *RegionServiceTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *RegionServiceTestSuite) Test_GetRegionById_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("GetRegionByID", uint(1)).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.GetRegionByID(uint(1))

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *RegionServiceTestSuite) Test_GetRegionByID_Found_ReturnsRegionModel() {
	//arrange
	region1 := suite.getTestRegion1()
	suite.repository.On("GetRegionByID", uint(1)).Return(region1, nil).Once()

	//act
	region, err := suite.service.GetRegionByID(uint(1))

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(region1, region)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *RegionServiceTestSuite) getTestRegion1() *models.Region {
	return &models.Region{uint(1), "Home region", uint(1), "My Country"}
}
