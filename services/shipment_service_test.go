package services

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common/db/config"
	"github.com/FoxComm/middlewarehouse/common/db/tasks"
	serviceMocks "github.com/FoxComm/middlewarehouse/controllers/mocks"
	"github.com/FoxComm/middlewarehouse/fixtures"
	"github.com/FoxComm/middlewarehouse/models"
	repositoryMocks "github.com/FoxComm/middlewarehouse/services/mocks"

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
	db                      *gorm.DB
}

func TestShipmentServiceSuite(t *testing.T) {
	suite.Run(t, new(ShipmentServiceTestSuite))
}

func (suite *ShipmentServiceTestSuite) SetupTest() {
	tasks.TruncateTables([]string{
		"shipments",
		"carriers",
		"shipping_methods",
		"shipment_line_items",
		"addresses",
	})

	var err error
	suite.db, err = config.DefaultConnection()
	suite.Nil(err)
	suite.shipmentRepository = &repositoryMocks.ShipmentRepositoryMock{}
	suite.addressService = &serviceMocks.AddressServiceMock{}
	suite.shipmentLineItemService = &serviceMocks.ShipmentLineItemServiceMock{}
	suite.service = NewShipmentService(suite.db, suite.shipmentRepository, suite.addressService, suite.shipmentLineItemService)
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
	carrier := &models.Carrier{Name: "USPS"}
	err := suite.db.Create(carrier).Error
	suite.Nil(err)

	method := &models.ShippingMethod{Name: "Standard Shipping", CarrierID: carrier.ID}
	err = suite.db.Create(method).Error
	suite.Nil(err)

	shipment1 := fixtures.GetShipmentShort(uint(0))
	shipment1.ShippingMethodID = method.ID

	//act
	shipment, err := suite.service.CreateShipment(shipment1)

	//assert
	suite.Nil(err)
	suite.Equal(shipment1, shipment)
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
