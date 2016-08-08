package services

import (
	"database/sql"
	"testing"

	"github.com/FoxComm/middlewarehouse/common/gormfox"
	"github.com/FoxComm/middlewarehouse/models"
	serviceMocks "github.com/FoxComm/middlewarehouse/controllers/mocks"
	repositoryMocks "github.com/FoxComm/middlewarehouse/services/mocks"

	"errors"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type ShipmentServiceTestSuite struct {
	GeneralServiceTestSuite
	shipmentRepository      *repositoryMocks.ShipmentRepositoryMock
	addressService          *serviceMocks.AddressServiceMock
	shipmentLineItemService *serviceMocks.ShipmentLineItemServiceMock
	service                 IShipmentService
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupTest() {
	suite.shipmentRepository = &repositoryMocks.ShipmentRepositoryMock{}
	suite.addressService = &serviceMocks.AddressServiceMock{}
	suite.shipmentLineItemService = &serviceMocks.ShipmentLineItemServiceMock{}
	suite.service = NewShipmentService(suite.shipmentRepository, suite.addressService, suite.shipmentLineItemService)

	suite.assert = assert.New(suite.T())
}

func (suite *ShipmentServiceTestSuite) TearDownTest() {
	// clear service mock calls expectations after each test
	suite.shipmentRepository.AssertExpectations(suite.T())
	suite.shipmentRepository.ExpectedCalls = []*mock.Call{}
	suite.shipmentRepository.Calls = []mock.Call{}

	suite.addressService.AssertExpectations(suite.T())
	suite.addressService.ExpectedCalls = []*mock.Call{}
	suite.addressService.Calls = []mock.Call{}

	suite.shipmentLineItemService.AssertExpectations(suite.T())
	suite.shipmentLineItemService.ExpectedCalls = []*mock.Call{}
	suite.shipmentLineItemService.Calls = []mock.Call{}
}

func (suite *ShipmentServiceTestSuite) Test_GetShipmentsByReferenceNumber_ReturnsShipmentModels() {
	//arrange
	shipment1 := suite.getTestShipment1()
	shipment2 := suite.getTestShipment2()
	suite.shipmentRepository.On("GetShipmentsByReferenceNumber", shipment1.ReferenceNumber).Return([]*models.Shipment{
		shipment1,
		shipment2,
	}, nil).Once()

	//act
	shipments, err := suite.service.GetShipmentsByReferenceNumber(shipment1.ReferenceNumber)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(2, len(shipments))
	suite.assert.Equal(shipment1, shipments[0])
	suite.assert.Equal(shipment2, shipments[1])
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_Succeed_ReturnsCreatedRecord() {
	//arrange
	shipment1 := suite.getTestShipment1()
	address1 := suite.getTestAddress1()
	shipmentLineItem1 := suite.getTestShipmentLineItem1(shipment1.ID)
	shipmentLineItem2 := suite.getTestShipmentLineItem2(shipment1.ID)
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(shipment1, nil).Once()
	suite.addressService.On("CreateAddress", address1).Return(address1, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", shipmentLineItem2).Return(shipmentLineItem2, nil).Once()

	//act
	shipment, err := suite.service.CreateShipment(shipment1, address1, []*models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2})

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shipment1, shipment)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_ShipmentFailure_PerformsRollback() {
	//arrange
	shipment1 := suite.getTestShipment1()
	address1 := suite.getTestAddress1()
	shipmentLineItem1 := suite.getTestShipmentLineItem1(shipment1.ID)
	shipmentLineItem2 := suite.getTestShipmentLineItem2(shipment1.ID)
	err1 := errors.New("some fail")
	suite.addressService.On("CreateAddress", address1).Return(address1, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(nil, err1).Once()
	suite.addressService.On("DeleteAddress", address1.ID).Return(nil).Once()

	//act
	_, err := suite.service.CreateShipment(shipment1, address1, []*models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2})

	//assert
	suite.assert.Equal(err1, err)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_LineItemFailure_PerformsRollback() {
	//arrange
	shipment1 := suite.getTestShipment1()
	address1 := suite.getTestAddress1()
	shipmentLineItem1 := suite.getTestShipmentLineItem1(shipment1.ID)
	shipmentLineItem2 := suite.getTestShipmentLineItem2(shipment1.ID)
	err1 := errors.New("some fail")
	suite.addressService.On("CreateAddress", address1).Return(address1, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(shipment1, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", shipmentLineItem1).Return(shipmentLineItem1, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", shipmentLineItem2).Return(nil, err1).Once()
	suite.addressService.On("DeleteAddress", address1.ID).Return(nil).Once()
	suite.shipmentRepository.On("DeleteShipment", shipment1.ID).Return(nil).Once()
	suite.shipmentLineItemService.On("DeleteShipmentLineItem", shipmentLineItem1.ID).Return(nil).Once()

	//act
	_, err := suite.service.CreateShipment(shipment1, address1, []*models.ShipmentLineItem{shipmentLineItem1, shipmentLineItem2})

	//assert
	suite.assert.Equal(err1, err)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.assert.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := suite.getTestShipment1()
	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(shipment1, nil).Once()

	//act
	shipment, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.assert.Nil(err)
	suite.assert.Equal(shipment1, shipment)
}

func (suite *ShipmentServiceTestSuite) getTestShipment1() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(1)}, uint(1), "BR1002", "pending",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *ShipmentServiceTestSuite) getTestShipment2() *models.Shipment {
	return &models.Shipment{gormfox.Base{ID: uint(2)}, uint(1), "BR1002", "shipped",
		sql.NullString{}, sql.NullString{}, sql.NullString{}, uint(1), sql.NullString{}}
}

func (suite *ShipmentServiceTestSuite) getTestAddress1() *models.Address {
	return &models.Address{gormfox.Base{ID: uint(1)}, "Home address", uint(1), "Texas", "75231",
		"Some st, 335", sql.NullString{String: "", Valid: false}, "19527352893"}
}

func (suite *ShipmentServiceTestSuite) getTestShipmentLineItem1(shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(1)}, shipmentID, "BR1002", "SKU-TEST1",
		"Some shit", 3999, "https://test.com/some-shit.png", "pending"}
}

func (suite *ShipmentServiceTestSuite) getTestShipmentLineItem2(shipmentID uint) *models.ShipmentLineItem {
	return &models.ShipmentLineItem{gormfox.Base{ID: uint(2)}, shipmentID, "BR1003", "SKU-TEST2",
		"Other shit", 4999, "https://test.com/other-shit.png", "delivered"}
}
