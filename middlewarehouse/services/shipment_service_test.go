package services

import (
	"errors"
	"testing"

	serviceMocks "github.com/FoxComm/highlander/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/highlander/middlewarehouse/fixtures"
	"github.com/FoxComm/highlander/middlewarehouse/models"
	repositoryMocks "github.com/FoxComm/highlander/middlewarehouse/services/mocks"

	"github.com/jinzhu/gorm"
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
	shipment1 := fixtures.GetShipmentShort(uint(1))
	shipment2 := fixtures.GetShipmentShort(uint(2))
	suite.shipmentRepository.On("GetShipmentsByReferenceNumber", shipment1.ReferenceNumber).Return([]*models.Shipment{
		shipment1,
		shipment2,
	}, nil).Once()

	//act
	shipments, err := suite.service.GetShipmentsByReferenceNumber(shipment1.ReferenceNumber)

	//assert
	suite.Nil(err)
	suite.Equal(2, len(shipments))
	suite.Equal(shipment1, shipments[0])
	suite.Equal(shipment2, shipments[1])
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_Succeed_ReturnsCreatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	createdShipment := fixtures.GetShipment(shipment1.ID, shipment1.ShippingMethodID, &models.ShippingMethod{}, shipment1.AddressID, &models.Address{}, []models.ShipmentLineItem{})
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(createdShipment, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[0]).Return(&shipment1.ShipmentLineItems[0], nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[1]).Return(&shipment1.ShipmentLineItems[1], nil).Once()
	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(shipment1, nil).Once()

	//act
	shipment, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_ShipmentFailure_PerformsRollback() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	err1 := errors.New("some fail")
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(nil, err1).Once()
	suite.addressService.On("DeleteAddress", shipment1.AddressID).Return(nil).Once()

	//act
	_, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Equal(err1, err)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_LineItemFailure_PerformsRollback() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	createdShipment := fixtures.GetShipment(shipment1.ID, shipment1.ShippingMethodID, &models.ShippingMethod{}, shipment1.AddressID, &models.Address{}, []models.ShipmentLineItem{})
	err1 := errors.New("some fail")
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(createdShipment, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[0]).Return(&shipment1.ShipmentLineItems[0], nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[1]).Return(nil, err1).Once()
	suite.addressService.On("DeleteAddress", shipment1.AddressID).Return(nil).Once()
	suite.shipmentRepository.On("DeleteShipment", shipment1.ID).Return(nil).Once()
	suite.shipmentLineItemService.On("DeleteShipmentLineItem", shipment1.ShipmentLineItems[0].ID).Return(nil).Once()

	//act
	_, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Equal(err1, err)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_NotFound_ReturnsNotFoundError() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(shipment1, nil).Once()
	suite.shipmentLineItemService.On("UpdateShipmentLineItem", &shipment1.ShipmentLineItems[0]).Return(&shipment1.ShipmentLineItems[0], nil).Once()
	suite.shipmentLineItemService.On("UpdateShipmentLineItem", &shipment1.ShipmentLineItems[1]).Return(&shipment1.ShipmentLineItems[1], nil).Once()
	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(shipment1, nil).Once()

	//act
	shipment, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
}
