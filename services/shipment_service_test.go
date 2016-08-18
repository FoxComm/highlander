package services

import (
	"testing"

	serviceMocks "github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"
	repositoryMocks "github.com/FoxComm/middlewarehouse/services/mocks"

	"errors"
	"fmt"
	"github.com/jinzhu/gorm"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type ShipmentServiceTestSuite struct {
	GeneralServiceTestSuite
	shipmentRepository      *repositoryMocks.ShipmentRepositoryMock
	addressService          *serviceMocks.AddressServiceMock
	shipmentLineItemService *serviceMocks.ShipmentLineItemServiceMock
	stockItemUnitRepository *repositoryMocks.StockItemUnitRepositoryMock
	service                 IShipmentService
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupTest() {
	suite.shipmentRepository = &repositoryMocks.ShipmentRepositoryMock{}
	suite.addressService = &serviceMocks.AddressServiceMock{}
	suite.shipmentLineItemService = &serviceMocks.ShipmentLineItemServiceMock{}
	suite.stockItemUnitRepository = &repositoryMocks.StockItemUnitRepositoryMock{}
	suite.service = NewShipmentService(
		suite.shipmentRepository,
		suite.addressService,
		suite.shipmentLineItemService,
		suite.stockItemUnitRepository,
	)
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

	suite.stockItemUnitRepository.AssertExpectations(suite.T())
	suite.stockItemUnitRepository.ExpectedCalls = []*mock.Call{}
	suite.stockItemUnitRepository.Calls = []mock.Call{}
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
	stockItemUnits := fixtures.GetStockItemUnits(fixtures.GetStockItem(uint(1), shipment1.ShipmentLineItems[0].SKU), 2)

	suite.stockItemUnitRepository.On("GetUnitsInOrder", shipment1.ReferenceNumber).Return(stockItemUnits).Once()
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(createdShipment, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[0]).Return(&shipment1.ShipmentLineItems[0], nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[1]).Return(&shipment1.ShipmentLineItems[1], nil).Once()
	suite.stockItemUnitRepository.On("ReserveUnitsInOrder", shipment1.ReferenceNumber).Return(2, nil)

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
	stockItemUnits := fixtures.GetStockItemUnits(fixtures.GetStockItem(uint(1), shipment1.ShipmentLineItems[0].SKU), 2)

	suite.stockItemUnitRepository.On("GetUnitsInOrder", shipment1.ReferenceNumber).Return(stockItemUnits).Once()
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(nil, err1).Once()
	suite.addressService.On("DeleteAddress", shipment1.AddressID).Return(nil).Once()

	//act
	_, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Equal(err1, err)
}

func (suite *ShipmentServiceTestSuite) Test_CreateShipment_StockItemsMatchingFailure_PerformsRollback() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))
	createdShipment := fixtures.GetShipment(shipment1.ID, shipment1.ShippingMethodID, &models.ShippingMethod{}, shipment1.AddressID, &models.Address{}, []models.ShipmentLineItem{})
	err1 := fmt.Errorf("Not found stock item unit with reference number %s and sku %s", shipment1.ReferenceNumber, shipment1.ShipmentLineItems[0].SKU)
	stockItemUnits := fixtures.GetStockItemUnits(fixtures.GetStockItem(uint(1), shipment1.ShipmentLineItems[0].SKU), 1)

	suite.stockItemUnitRepository.On("GetUnitsInOrder", shipment1.ReferenceNumber).Return(stockItemUnits).Once()
	suite.addressService.On("CreateAddress", &shipment1.Address).Return(&shipment1.Address, nil).Once()
	suite.shipmentRepository.On("CreateShipment", shipment1).Return(createdShipment, nil).Once()
	suite.shipmentLineItemService.On("CreateShipmentLineItem", &shipment1.ShipmentLineItems[0]).Return(&shipment1.ShipmentLineItems[0], nil).Once()
	suite.addressService.On("DeleteAddress", shipment1.AddressID).Return(nil).Once()
	suite.shipmentRepository.On("DeleteShipment", shipment1.ID).Return(nil).Once()
	suite.shipmentLineItemService.On("DeleteShipmentLineItem", shipment1.ShipmentLineItems[0].ID).Return(nil).Once()

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
	stockItemUnits := fixtures.GetStockItemUnits(fixtures.GetStockItem(uint(1), shipment1.ShipmentLineItems[0].SKU), 2)

	suite.stockItemUnitRepository.On("GetUnitsInOrder", shipment1.ReferenceNumber).Return(stockItemUnits).Once()
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

	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(nil, gorm.ErrRecordNotFound).Once()

	//act
	_, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.Equal(gorm.ErrRecordNotFound, err)
}

func (suite *ShipmentServiceTestSuite) Test_UpdateShipment_Found_ReturnsUpdatedRecord() {
	//arrange
	shipment1 := fixtures.GetShipmentShort(uint(1))

	suite.shipmentRepository.On("GetShipmentByID", shipment1.ID).Return(shipment1, nil).Once()
	suite.shipmentRepository.On("UpdateShipment", shipment1).Return(shipment1, nil).Once()

	//act
	shipment, err := suite.service.UpdateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
}
