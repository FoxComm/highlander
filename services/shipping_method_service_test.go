package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type ShippingMethodServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.ShippingMethodRepositoryMock
	service    IShippingMethodService
}

func TestShippingMethodServiceSuite(t *testing.T) {
	suite.Run(t, new(ShippingMethodServiceTestSuite))
}

func (suite *ShippingMethodServiceTestSuite) SetupTest() {
	suite.repository = &mocks.ShippingMethodRepositoryMock{}
	suite.service = NewShippingMethodService(suite.repository)

	suite.assert = assert.New(suite.T())
}

func (suite *ShippingMethodServiceTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{
		CarrierID: uint(1),
		Name:      "UPS 2 days ground",
	}
	shippingMethod2 := &models.ShippingMethod{
		CarrierID: uint(1),
		Name:      "DHL 2 days ground",
	}
	suite.repository.On("GetShippingMethods").Return([]*models.ShippingMethod{
		shippingMethod1,
		shippingMethod2,
	}, nil).Once()

	//act
	shippingMethods, err := suite.service.GetShippingMethods()

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(shippingMethods))
	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethods[0].CarrierID)
	suite.assert.Equal(shippingMethod1.Name, shippingMethods[0].Name)
	suite.assert.Equal(shippingMethod2.CarrierID, shippingMethods[1].CarrierID)
	suite.assert.Equal(shippingMethod2.Name, shippingMethods[1].Name)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodById_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := &models.ShippingMethod{
		CarrierID: uint(1),
		Name:      "UPS 2 days ground",
	}
	suite.repository.On("GetShippingMethodByID").Return(shippingMethod1, nil).Once()

	//act
	shippingMethod, err := suite.service.GetShippingMethodByID(1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shippingMethod1.CarrierID, shippingMethod.CarrierID)
	suite.assert.Equal(shippingMethod1.Name, shippingMethod.Name)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *ShippingMethodServiceTestSuite) Test_CreaterShippingMethod_ReturnsIdOfCreatedRecord() {
	//arrange
	carrierID, name := uint(1), "UPS 2 days ground"
	model := &models.ShippingMethod{CarrierID: carrierID, Name: name}
	suite.repository.On("CreateShippingMethod").Return(uint(1), nil).Once()

	//act
	id, err := suite.service.CreateShippingMethod(model)

	//assert
	suite.assert.Equal(uint(1), id)
	suite.assert.Nil(err)
	suite.repository.AssertExpectations(suite.T())
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_ReturnsNoError() {
	//arrange
	carrierID, name := uint(1), "UPS 2 days ground"
	suite.repository.On("UpdateShippingMethod").Return(true).Once()

	//act
	err := suite.service.UpdateShippingMethod(&models.ShippingMethod{ID: 1, CarrierID: carrierID, Name: name})

	//assert
	suite.assert.Nil(err)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteShippingMethod").Return(true).Once()

	//act
	err := suite.service.DeleteShippingMethod(1)

	//assert
	suite.assert.Nil(err)
}
