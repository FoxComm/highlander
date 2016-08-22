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

type ShipmentLineItemServiceTestSuite struct {
	GeneralServiceTestSuite
	repository *mocks.ShipmentLineItemRepositoryMock
	service    IShipmentLineItemService
}

func TestShipmentLineItemServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentLineItemServiceTestSuite))
}

func (suite *ShipmentLineItemServiceTestSuite) SetupTest() {
	suite.repository = &mocks.ShipmentLineItemRepositoryMock{}
	suite.service = NewShipmentLineItemService(suite.repository)
}

func (suite *ShipmentLineItemServiceTestSuite) TearDownTest() {
	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
	// clear service mock calls expectations after each test
	suite.repository.ExpectedCalls = []*mock.Call{}
	suite.repository.Calls = []mock.Call{}
}

func (suite *ShipmentLineItemServiceTestSuite) Test_GetShipmentLineItemsByShipmentID_ReturnsShipmentLineItemModels() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	shipmentLineItem2 := fixtures.GetShipmentLineItem(uint(2), uint(1))
	suite.repository.On("GetShipmentLineItemsByShipmentID", uint(1)).Return([]*models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2}, nil).Once()

	//act
	shipmentLineItems, err := suite.service.GetShipmentLineItemsByShipmentID(uint(1))

	//assert
	suite.Nil(err)

	suite.Equal(2, len(shipmentLineItems))
	suite.Equal(shipmentLineItem1, shipmentLineItems[0])
	suite.Equal(shipmentLineItem2, shipmentLineItems[1])
}

func (suite *ShipmentLineItemServiceTestSuite) Test_CreateShipmentLineItem_ReturnsCreatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.repository.On("CreateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()

	//act
	shipmentLineItem, err := suite.service.CreateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.Nil(err)
	suite.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_UpdateShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.repository.On("UpdateShipmentLineItem", shipmentLineItem1).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_UpdateShipmentLineItem_Found_ReturnsUpdatedRecord() {
	//arrange
	shipmentLineItem1 := fixtures.GetShipmentLineItem(uint(1), uint(1))
	suite.repository.On("UpdateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()

	//act
	shipmentLineItem, err := suite.service.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.Nil(err)
	suite.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_DeleteShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("DeleteShipmentLineItem", uint(1)).Return(gorm.ErrRecordNotFound).Once()

	//act
	err := suite.service.DeleteShipmentLineItem(uint(1))

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_DeleteShipmentLineItem_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteShipmentLineItem", uint(1)).Return(nil).Once()

	//act
	err := suite.service.DeleteShipmentLineItem(uint(1))

	//assert
	suite.Nil(err)
}
