package services

import (
	"testing"

	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	"github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"fmt"
	"github.com/FoxComm/highlander/middlewarehouse/repositories"
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
}

func (suite *ShippingMethodServiceTestSuite) TearDownTest() {
	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())

	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethods_ReturnsShippingMethodModels() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	shippingMethod2 := fixtures.GetShippingMethod(uint(2), uint(2), fixtures.GetCarrier(uint(2)))
	suite.repository.On("GetShippingMethods").Return([]*models.ShippingMethod{shippingMethod1, shippingMethod2}, nil).Once()

	//act
	shippingMethods, exception := suite.service.GetShippingMethods()

	//assert
	suite.Nil(exception)

	suite.Equal(2, len(shippingMethods))
	suite.Equal(shippingMethod1, shippingMethods[0])
	suite.Equal(shippingMethod2, shippingMethods[1])
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodByID_NotFound_ReturnsNotFoundError() {
	//arrange
	ex := repositories.NewEntityNotFoundException(repositories.ShippingMethodEntity, "1", fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1))
	suite.repository.On("GetShippingMethodByID", uint(1)).Return(nil, ex).Once()

	//act
	_, exception := suite.service.GetShippingMethodByID(uint(1))

	//assert
	suite.Equal(ex, exception)
}

func (suite *ShippingMethodServiceTestSuite) Test_GetShippingMethodByID_Found_ReturnsShippingMethodModel() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.repository.On("GetShippingMethodByID", shippingMethod1.ID).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod, exception := suite.service.GetShippingMethodByID(shippingMethod1.ID)

	//assert
	suite.Nil(exception)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_CreateShippingMethod_ReturnsCreatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.repository.On("CreateShippingMethod", shippingMethod1).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod, exception := suite.service.CreateShippingMethod(shippingMethod1)

	//assert
	suite.Nil(exception)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	ex := repositories.NewEntityNotFoundException(repositories.ShippingMethodEntity, "1", fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1))
	suite.repository.On("UpdateShippingMethod", shippingMethod1).Return(nil, ex).Once()

	//act
	_, exception := suite.service.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Equal(ex, exception)
}

func (suite *ShippingMethodServiceTestSuite) Test_UpdateShippingMethod_Found_ReturnsUpdatedRecord() {
	//arrange
	shippingMethod1 := fixtures.GetShippingMethod(uint(1), uint(1), fixtures.GetCarrier(uint(1)))
	suite.repository.On("UpdateShippingMethod", shippingMethod1).Return(shippingMethod1, nil).Once()

	//act
	shippingMethod, exception := suite.service.UpdateShippingMethod(shippingMethod1)

	//assert
	suite.Nil(exception)
	suite.Equal(shippingMethod1, shippingMethod)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_NotFound_ReturnsNotFoundError() {
	//arrange
	ex := repositories.NewEntityNotFoundException(repositories.ShippingMethodEntity, "1", fmt.Errorf(repositories.ErrorShippingMethodNotFound, 1))
	suite.repository.On("DeleteShippingMethod", uint(1)).Return(ex).Once()

	//act
	exception := suite.service.DeleteShippingMethod(uint(1))

	//assert
	suite.Equal(ex, exception)
}

func (suite *ShippingMethodServiceTestSuite) Test_DeleteShippingMethod_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteShippingMethod", uint(1)).Return(nil).Once()

	//act
	exception := suite.service.DeleteShippingMethod(uint(1))

	//assert
	suite.Nil(exception)
}
