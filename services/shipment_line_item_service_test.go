package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/models"
	"github.com/FoxComm/middlewarehouse/services/mocks"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"github.com/jinzhu/gorm"
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

	suite.assert = assert.New(suite.T())
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
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	shipmentLineItem2 := suite.getTestShipmentLineItem2()
	suite.repository.On("GetShipmentLineItemsByShipmentID", uint(1)).Return([]*models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2}, nil).Once()

	//act
	shipmentLineItems, err := suite.service.GetShipmentLineItemsByShipmentID(uint(1))

	//assert
	suite.assert.Nil(err)

	suite.assert.Equal(2, len(shipmentLineItems))
	suite.assert.Equal(shipmentLineItem1, shipmentLineItems[0])
	suite.assert.Equal(shipmentLineItem2, shipmentLineItems[1])
}

func (suite *ShipmentLineItemServiceTestSuite) Test_CreateShipmentLineItem_ReturnsCreatedRecord() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.repository.On("CreateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()

	//act
	shipmentLineItem, err := suite.service.CreateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_UpdateShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.repository.On("UpdateShipmentLineItem", shipmentLineItem1).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_UpdateShipmentLineItem_Found_ReturnsUpdatedRecord() {
	//arrange
	shipmentLineItem1 := suite.getTestShipmentLineItem1()
	suite.repository.On("UpdateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()

	//act
	shipmentLineItem, err := suite.service.UpdateShipmentLineItem(shipmentLineItem1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shipmentLineItem1, shipmentLineItem)
}

func (suite *ShipmentLineItemServiceTestSuite) Test_DeleteShipmentLineItem_NotFound_ReturnsNotFoundError() {
	//arrange
	suite.repository.On("DeleteShipmentLineItem", uint(1)).Return(false, gorm.ErrRecordNotFound).Once()

	//act
	err := suite.service.DeleteShipmentLineItem(uint(1))

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *ShipmentLineItemServiceTestSuite) Test_DeleteShipmentLineItem_Found_ReturnsNoError() {
	//arrange
	suite.repository.On("DeleteShipmentLineItem", uint(1)).Return(true).Once()

	//act
	err := suite.service.DeleteShipmentLineItem(uint(1))

	//assert
	suite.assert.Nil(err)

	//assert all expectations were met
	suite.repository.AssertExpectations(suite.T())
}

func (suite *ShipmentLineItemServiceTestSuite) getTestShipmentLineItem1() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(1)}, uint(1), "BR1002", "SKU-TEST1",
		"Some shit", 3999, "https://test.com/some-shit.png", "pending"}
}

func (suite *ShipmentLineItemServiceTestSuite) getTestShipmentLineItem2() *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(2)}, uint(1), "BR1003", "SKU-TEST2",
		"Other shit", 4999, "https://test.com/other-shit.png", "delivered"}
}
